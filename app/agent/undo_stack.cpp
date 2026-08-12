#include <mutex>
#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <stack>
#include <string>
#include <iostream>

namespace py = pybind11;

struct ActionNode {
    std::string action_name;
    std::string undo_payload_json;
};

class UndoStackCPP {
private:
    std::stack<ActionNode> stack;

public:
    UndoStackCPP() {}

    void push(const std::string& action_name, const std::string& undo_payload_json) {
        stack.push({action_name, undo_payload_json});
        std::cout << "📌 [C++ STACK PUSH] Registered action: " << action_name << std::endl;
    }

    py::dict pop_and_undo() {
        py::dict result;
        if (stack.empty()) {
            result["status"] = "empty";
            result["message"] = "No actions available to undo.";
            return result;
        }

        ActionNode top = stack.top();
        stack.pop();

        result["status"] = "success";
        result["action_name"] = top.action_name;
        result["undo_payload_json"] = top.undo_payload_json;
        std::cout << "⏪ [C++ STACK POP] Popped action: " << top.action_name << std::endl;
        return result;
    }

    std::string peek() {
        if (stack.empty()) return "Empty";
        return stack.top().action_name;
    }
};

PYBIND11_MODULE(dsa_engine, m) {
    py::class_<UndoStackCPP>(m, "UndoStackCPP")
        .def(py::init<>())
        .def("push", &UndoStackCPP::push)
        .def("pop_and_undo", &UndoStackCPP::pop_and_undo)
        .def("peek", &UndoStackCPP::peek);
}