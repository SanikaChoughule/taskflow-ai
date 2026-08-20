#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <stack>
#include <queue>
#include <vector>
#include <string>
#include <iostream>
#include <functional>

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

struct ReminderNode {
    long long trigger_time;
    std::string reminder_id;
    std::string task;

    // Standard min-heap comparison (smaller trigger_time has higher priority)
    bool operator>(const ReminderNode& other) const {
        return trigger_time > other.trigger_time;
    }
};

class ReminderPriorityQueueCPP {
private:
    std::priority_queue<ReminderNode, std::vector<ReminderNode>, std::greater<ReminderNode>> queue;

public:
    ReminderPriorityQueueCPP() {}

    void push(long long trigger_time, const std::string& reminder_id, const std::string& task) {
        queue.push({trigger_time, reminder_id, task});
    }

    py::dict pop() {
        py::dict result;
        if (queue.empty()) {
            result["status"] = "empty";
            return result;
        }
        ReminderNode top = queue.top();
        queue.pop();
        result["status"] = "success";
        result["trigger_time"] = top.trigger_time;
        result["reminder_id"] = top.reminder_id;
        result["task"] = top.task;
        return result;
    }

    py::dict peek() {
        py::dict result;
        if (queue.empty()) {
            result["status"] = "empty";
            return result;
        }
        ReminderNode top = queue.top();
        result["status"] = "success";
        result["trigger_time"] = top.trigger_time;
        result["reminder_id"] = top.reminder_id;
        result["task"] = top.task;
        return result;
    }

    int size() {
        return queue.size();
    }

    bool empty() {
        return queue.empty();
    }
};

PYBIND11_MODULE(dsa_engine, m) {
    py::class_<UndoStackCPP>(m, "UndoStackCPP")
        .def(py::init<>())
        .def("push", &UndoStackCPP::push)
        .def("pop_and_undo", &UndoStackCPP::pop_and_undo)
        .def("peek", &UndoStackCPP::peek);

    py::class_<ReminderPriorityQueueCPP>(m, "ReminderPriorityQueueCPP")
        .def(py::init<>())
        .def("push", &ReminderPriorityQueueCPP::push)
        .def("pop", &ReminderPriorityQueueCPP::pop)
        .def("peek", &ReminderPriorityQueueCPP::peek)
        .def("size", &ReminderPriorityQueueCPP::size)
        .def("empty", &ReminderPriorityQueueCPP::empty);
}