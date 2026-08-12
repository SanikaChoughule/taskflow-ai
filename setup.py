import setuptools
from pybind11.setup_helpers import Pybind11Extension, build_ext

ext_modules = [
    Pybind11Extension(
        "app.agent.dsa_engine",
        ["app/agent/undo_stack.cpp"],
        extra_compile_args=["-std=c++17"],
    ),
]

setuptools.setup(
    name="dsa_engine",
    ext_modules=ext_modules,
    cmdclass={"build_ext": build_ext},
)