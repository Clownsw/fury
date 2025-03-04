#!/usr/bin/env bash

# Copyright 2023 The Fury Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


set -x

# Cause the script to exit if a single command fails.
set -e

# configure ~/.pypirc before run this script
#if [ ! -f ~/.pypirc ]; then
#  echo  "Please configure .pypirc before run this script"
#  exit 1
#fi

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"
WHEEL_DIR="$ROOT/.whl"

PYTHONS=("cp37-cp37m"
         "cp38-cp38"
         "cp39-cp39"
         "cp310-cp310"
         "cp310-cp311"
         "cp312-cp312")

VERSIONS=("3.7"
          "3.8"
          "3.9"
          "3.10"
          "3.11"
          "3.12")

source $(conda info --base)/etc/profile.d/conda.sh

create_py_envs() {
  for version in "${VERSIONS[@]}"; do
    conda create -y --name "py$version" python="$version"
  done
  conda env list
}

rename_linux_wheels() {
  for path in "$1"/*.whl; do
    if [ -f "${path}" ]; then
      mv "${path}" "${path//linux/manylinux1}"
    fi
  done
}

rename_mac_wheels() {
  for path in "$WHEEL_DIR"/*.whl; do
    if [ -f "${path}" ]; then
      cp "${path}" "${path//macosx_12_0_x86_64/macosx_10_13_x86_64}"
    fi
  done
}

bump_version() {
  version=$1
  bump_java_version "$version"
  bump_py_version "$version"
  bump_javascript_version "$version"
}

bump_java_version() {
  version=$1
  cd "$ROOT/java"
  echo "Set fury java version to $version"
  mvn versions:set -DnewVersion="$version"
  echo "Set fury integration_tests version to $version"
  cd "$ROOT/integration_tests/jdk_compatibility_tests"
  mvn versions:set -DnewVersion="$version"
  cd "$ROOT/integration_tests/perftests"
  mvn versions:set -DnewVersion="$version"
  cd "$ROOT/integration_tests/latest_jdk_tests"
  mvn versions:set -DnewVersion="$version"
}

bump_py_version() {
  version=$1
  echo "Set fury python version to $version"
  cd "$ROOT/python/pyfury"
  pyversion=${version/-alpha./.a}  # version will override
  pyversion=${pyversion/-beta./.b}
  echo "Bump fury python version to $pyversion"
  sed -i -E "s/__version__ = .*/__version__ = \"$pyversion\"/" __init__.py
}

bump_javascript_version() {
  version=$1
  cd "$ROOT/javascript"
  echo "Set fury javascript version to $version"
  pushd packages/fury
  sed -i -E "s/\"version\": .*,/\"version\": \"$version\",/" package.json
  popd
  pushd packages/hps
  sed -i -E "s/\"version\": .*,/\"version\": \"$version\",/" package.json
  popd
}

deploy_jars() {
  cd "$ROOT/java"
  mvn -T10 clean deploy --no-transfer-progress -DskipTests -Prelease
}

deploy_python() {
  if command -v pyenv; then
    pyenv local system
  fi
  cd "$ROOT/python"
  rm -rf "$WHEEL_DIR"
  mkdir -p "$WHEEL_DIR"
  for ((i=0; i<${#PYTHONS[@]}; ++i)); do
    PYTHON=${PYTHONS[i]}
    ENV="py${VERSIONS[i]}"
    conda activate "$ENV"
    python -V
    git clean -f -f -x -d -e .whl
    # Ensure bazel select the right version of python
    bazel clean --expunge
    install_pyarrow
    pip install --ignore-installed twine setuptools cython numpy
    pyarrow_dir=$(python -c "import importlib.util; import os; print(os.path.dirname(importlib.util.find_spec('pyarrow').origin))")
    # ensure pyarrow is clean
    rm -rf "$pyarrow_dir"
    pip install --ignore-installed pyarrow==$pyarrow_version
    python setup.py clean
    python setup.py bdist_wheel
    mv dist/pyfury*.whl "$WHEEL_DIR"
  done
  if [[ "$OSTYPE" == "linux"* ]]; then
    rename_linux_wheels "$WHEEL_DIR"
  fi
  twine check "$WHEEL_DIR"/pyfury*.whl
  twine upload -r pypi "$WHEEL_DIR"/pyfury*.whl
}

install_pyarrow() {
  pyversion=$(python -V | cut -d' ' -f2)
  if [[ $pyversion  ==  3.7* ]]; then
    pyarrow_version=12.0.0
    sed -i -E "s/pyarrow_version = .*/pyarrow_version = \"12.0.0\"/" "$ROOT"/python/setup.py
  else
    pyarrow_version=14.0.0
  fi
  pip install pyarrow==$pyarrow_version
}

case "$1" in
java) # Deploy jars to maven repository.
  deploy_jars
  ;;
python) # Deploy wheel to pypi
  deploy_python
  ;;
*)
  echo "Execute command $*"
  "$@"
  ;;
esac