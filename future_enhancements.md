# Future Enhancements: DSA Visualizer Panel

This document outlines the design and specifications for the **DSA Visualizer Panel**, to be implemented in a subsequent phase of TaskFlow AI.

---

## 1. Visual LIFO Undo Stack
- **Concept**: A visual stack representing the active `global_undo_stack`.
- **UI Element**: A vertical container of cards where each scheduled action (e.g. calendar creation or reminder creation) is represented as a floating card.
- **Interactions**:
  - Pushing an item onto the stack adds a card to the top with a slide-down animation.
  - Calling "Undo" pops the card off the top of the stack with a fade-out slide-up animation.

## 2. Visual Priority Queue Min-Heap (Scheduler)
- **Concept**: A sorted list showing the upcoming reminders in priority order.
- **UI Element**: A card list sorted by trigger timestamp, showing:
  - The reminder title.
  - A real-time countdown timer ticking down to zero.
  - The next reminder to execute highlighted at the top (the heap root).
- **Interactions**:
  - When the countdown reaches zero, the item pulses, fires a sound alert, and is removed from the queue.

## 3. Visual Task Dependency Graph (DAG)
- **Concept**: A connected flowchart representing multi-step execution.
- **UI Element**: A graph structure using node cards connected by arrows representing parent-child execution paths.
- **Interactions**:
  - Each task node displays a status badge matching its execution state: `pending` (gray), `running` (glowing orange), `completed` (green), or `failed` (red).
  - Transition animations trigger as sub-tasks resolve sequentially or in parallel.

## 4. Proactive Overrun Delay Action & Contact Mapping
- **Concept**: Expand the Proactive Overrun Checker to draft and send real emails to meeting attendees or organizers.
- **Features**:
  - Store third-party contact credentials and mapping databases linking organizer emails to contact names.
  - Automatically draft a standard delay notification (e.g., *"I will be 5 minutes late"*) and dispatch it via Google Gmail API or direct SMTP mail transport when the user clicks **Draft Email**.
