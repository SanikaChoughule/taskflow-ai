import setuptools
from setuptools.command.build_ext import build_ext
import pybind11

class BuildExt(build_ext):
    def build_extensions(self):
        compiler_type = self.compiler.compiler_type
        for ext in self.extensions:
            if compiler_type == 'mingw32':
                ext.extra_compile_args = ['-std=c++17', '-O3']
            else:
                ext.extra_compile_args = ['/std:c++17', '/EHsc', '/O2']
        super().build_extensions()

ext_modules = [
    setuptools.Extension(
        "app.agent.dsa_engine",
        ["app/agent/undo_stack.cpp"],
        include_dirs=[
            pybind11.get_include(),
            pybind11.get_include(user=True),
        ],
        language="c++",
    ),
]

setuptools.setup(
    name="dsa_engine",
    ext_modules=ext_modules,
    cmdclass={"build_ext": BuildExt},
)