require('dotenv').config();
const express = require('express');
const cors = require('cors');
const mysql = require('mysql2/promise');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// ---------------------------------------------------------------------------
// Helpers to format dates safely for MySQL DATETIME and DATE
// ---------------------------------------------------------------------------
function toMySQLDateTime(val) {
    if (!val) return null;
    let d;
    if (typeof val === 'number') {
        d = new Date(val);
    } else if (typeof val === 'string') {
        const num = Number(val);
        if (!isNaN(num) && num > 10000000000) {
            d = new Date(num);
        } else {
            d = new Date(val);
        }
    } else {
        d = new Date(val);
    }
    if (isNaN(d.getTime())) return null;
    return d.toISOString().slice(0, 19).replace('T', ' ');
}

function toMySQLDate(val, fallbackMs) {
    if (fallbackMs) {
        const d = new Date(Number(fallbackMs));
        if (!isNaN(d.getTime())) {
            return d.toISOString().slice(0, 10);
        }
    }
    if (!val) return null;
    const d = new Date(val);
    if (!isNaN(d.getTime())) {
        return d.toISOString().slice(0, 10);
    }
    return new Date().toISOString().slice(0, 10);
}

// ---------------------------------------------------------------------------
// MySQL Connection Pool
// ---------------------------------------------------------------------------
const pool = mysql.createPool({
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT || '3306'),
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'taskflow_db',
    waitForConnections: true,
    connectionLimit: 10,
    queueLimit: 0
});

// Test connection on startup
(async () => {
    try {
        const connection = await pool.getConnection();
        console.log(`[TaskFlow Backend] Connected to MySQL database "${process.env.DB_NAME || 'taskflow_db'}" successfully!`);
        connection.release();
    } catch (err) {
        console.error('[TaskFlow Backend] Error connecting to MySQL:', err.message);
        console.log('Please make sure MySQL is running and check your .env credentials.');
    }
})();

// ---------------------------------------------------------------------------
// HEALTH CHECK
// ---------------------------------------------------------------------------
app.get('/health', (req, res) => {
    res.json({ status: 'UP', service: 'TaskFlow AI Backend', timestamp: Date.now() });
});

// ---------------------------------------------------------------------------
// 1. SYNC / REGISTER USER
// ---------------------------------------------------------------------------
app.post('/api/v1/auth/sync', async (req, res) => {
    try {
        const { id, email, name, photoUrl, isGoogleConnected, isCalendarConnected, lastLoginAt } = req.body;
        console.log(`[TaskFlow Backend] Received auth/sync for: ${email} (${id})`);

        if (!id || !email) {
            return res.status(400).json({ error: 'Missing required user id or email' });
        }

        const formattedLastLogin = toMySQLDateTime(lastLoginAt) || toMySQLDateTime(Date.now());

        const sql = `
            INSERT INTO users (id, email, name, photo_url, is_google_connected, is_calendar_connected, last_login_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                name = VALUES(name),
                photo_url = VALUES(photo_url),
                is_google_connected = VALUES(is_google_connected),
                is_calendar_connected = VALUES(is_calendar_connected),
                last_login_at = VALUES(last_login_at);
        `;
        await pool.execute(sql, [
            id,
            email,
            name || email.split('@')[0],
            photoUrl || null,
            isGoogleConnected ? 1 : 0,
            isCalendarConnected ? 1 : 0,
            formattedLastLogin
        ]);

        console.log(`[TaskFlow Backend] SUCCESS: User "${email}" saved to MySQL users table!`);
        res.json({ success: true, message: 'User synchronized successfully' });
    } catch (err) {
        console.error('[TaskFlow Backend] Error syncing user:', err.message);
        res.status(500).json({ error: err.message });
    }
});

// ---------------------------------------------------------------------------
// 2. TASKS ENDPOINTS (Per-User Isolation)
// ---------------------------------------------------------------------------

// GET tasks for specific user
app.get('/api/v1/tasks', async (req, res) => {
    try {
        const { userId } = req.query;
        if (!userId) {
            return res.status(400).json({ error: 'userId query parameter is required' });
        }

        const [rows] = await pool.execute(
            'SELECT * FROM tasks WHERE user_id = ? ORDER BY created_at DESC',
            [userId]
        );
        res.json(rows);
    } catch (err) {
        console.error('[TaskFlow Backend] Error fetching tasks:', err.message);
        res.status(500).json({ error: err.message });
    }
});

// CREATE task for specific user
app.post('/api/v1/tasks', async (req, res) => {
    try {
        const {
            id, userId, title, description,
            status, priority, completedAt, dueDate
        } = req.body;

        if (!id || !userId || !title) {
            return res.status(400).json({ error: 'id, userId, and title are required' });
        }

        const formattedCompletedAt = toMySQLDateTime(completedAt);
        const formattedDueDate = toMySQLDateTime(dueDate);

        const sql = `
            INSERT INTO tasks (
                id, user_id, title, description,
                status, priority, due_date, completed_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                title = VALUES(title),
                description = VALUES(description),
                status = VALUES(status),
                priority = VALUES(priority),
                due_date = VALUES(due_date),
                completed_at = VALUES(completed_at);
        `;

        await pool.execute(sql, [
            id,
            userId,
            title,
            description || '',
            status || 'PENDING',
            priority || 'MEDIUM',
            formattedDueDate,
            formattedCompletedAt
        ]);

        console.log(`[TaskFlow Backend] SUCCESS: Task "${title}" saved for user ${userId}`);
        res.status(201).json({ success: true, id, message: 'Task saved for user' });
    } catch (err) {
        console.error('[TaskFlow Backend] Error saving task:', err.message);
        res.status(500).json({ error: err.message });
    }
});

// ---------------------------------------------------------------------------
// 3. SCHEDULE HISTORY ENDPOINTS (Per-User Isolation)
// ---------------------------------------------------------------------------

// GET history for specific user
app.get('/api/v1/history', async (req, res) => {
    try {
        const { userId } = req.query;
        if (!userId) {
            return res.status(400).json({ error: 'userId query parameter is required' });
        }

        const [rows] = await pool.execute(
            'SELECT * FROM schedule_history WHERE user_id = ? ORDER BY action_time DESC',
            [userId]
        );
        res.json(rows);
    } catch (err) {
        console.error('[TaskFlow Backend] Error fetching history:', err.message);
        res.status(500).json({ error: err.message });
    }
});

// RECORD scheduled event for specific user
app.post('/api/v1/history', async (req, res) => {
    try {
        const {
            id, userId, title, scheduledDate, startTimeFormatted,
            endTimeFormatted, startTimeMs, endTimeMs,
            attendee, attendeeEmail, action, actionTime, calendarEventId
        } = req.body;

        if (!id || !userId || !title) {
            return res.status(400).json({ error: 'id, userId, and title are required' });
        }

        const formattedDate = toMySQLDate(scheduledDate, startTimeMs);
        const formattedActionTime = toMySQLDateTime(actionTime) || toMySQLDateTime(Date.now());

        const sql = `
            INSERT INTO schedule_history (
                id, user_id, title, scheduled_date, start_time_formatted,
                end_time_formatted, start_time_ms, end_time_ms,
                attendee, attendee_email, action, action_time, calendar_event_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                action = VALUES(action),
                action_time = VALUES(action_time);
        `;

        await pool.execute(sql, [
            id,
            userId,
            title,
            formattedDate,
            startTimeFormatted || '',
            endTimeFormatted || '',
            startTimeMs || 0,
            endTimeMs || 0,
            attendee || null,
            attendeeEmail || null,
            action || 'SCHEDULED',
            formattedActionTime,
            calendarEventId || ''
        ]);

        console.log(`[TaskFlow Backend] SUCCESS: Meeting "${title}" saved for user ${userId}`);
        res.status(201).json({ success: true, id, message: 'History recorded for user' });
    } catch (err) {
        console.error('[TaskFlow Backend] Error saving history:', err.message);
        res.status(500).json({ error: err.message });
    }
});

// MARK meeting undone for specific user
app.post('/api/v1/history/undo', async (req, res) => {
    try {
        const { calendarEventId, userId } = req.body;
        if (!calendarEventId) {
            return res.status(400).json({ error: 'calendarEventId is required' });
        }

        const nowFormatted = toMySQLDateTime(Date.now());
        let sql = 'UPDATE schedule_history SET action = ?, action_time = ? WHERE calendar_event_id = ?';
        const params = ['UNDONE', nowFormatted, calendarEventId];

        if (userId) {
            sql += ' AND user_id = ?';
            params.push(userId);
        }

        const [result] = await pool.execute(sql, params);
        res.json({ success: true, updated: result.affectedRows });
    } catch (err) {
        console.error('[TaskFlow Backend] Error marking undone:', err.message);
        res.status(500).json({ error: err.message });
    }
});

// CLEAR history for user
app.delete('/api/v1/history', async (req, res) => {
    try {
        const { userId } = req.query;
        if (!userId) {
            return res.status(400).json({ error: 'userId is required' });
        }

        await pool.execute('DELETE FROM schedule_history WHERE user_id = ?', [userId]);
        res.json({ success: true, message: 'User history cleared' });
    } catch (err) {
        console.error('[TaskFlow Backend] Error clearing history:', err.message);
        res.status(500).json({ error: err.message });
    }
});

// ---------------------------------------------------------------------------
// START SERVER (Listen on 0.0.0.0 to accept phone connections)
// ---------------------------------------------------------------------------
app.listen(PORT, '0.0.0.0', () => {
    console.log(`TaskFlow Backend running at http://0.0.0.0:${PORT}`);
    console.log(`Accepting connections from mobile devices on local Wi-Fi!`);
});
