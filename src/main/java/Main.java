package com.todo;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Main {
    private static final String DB_NAME = "todo.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_NAME;

    public static void main(String[] args) {
        try {
            // 加载 SQLite JDBC 驱动
            Class.forName("org.sqlite.JDBC");
            
            // 检查数据库文件是否存在
            File dbFile = new File(DB_NAME);
            boolean needCreateTable = !dbFile.exists();
            
            // 测试数据库连接
            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                if (needCreateTable) {
                    createDatabase();
                }
            }
            
            // 启动提醒任务
            ReminderTask reminderTask = new ReminderTask();
            reminderTask.startMonitoring();
            
            System.out.println("提醒服务已启动...");
            
            // 保持程序运行
            while (true) {
                Thread.sleep(1000);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS todo_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER NOT NULL," +
                "task TEXT NOT NULL," +
                "password TEXT NOT NULL," +
                "api TEXT NOT NULL," +
                "reminder_time_1 TEXT NOT NULL," +
                "reminder_time_2 TEXT," +
                "reminder_time_3 TEXT," +
                "execution_status_1 TEXT NOT NULL," +
                "execution_status_2 TEXT," +
                "execution_status_3 TEXT," +
                "api_response_1 TEXT," +
                "api_response_2 TEXT," +
                "api_response_3 TEXT," +
                "retry_count_1 INTEGER," +
                "retry_count_2 INTEGER," +
                "retry_count_3 INTEGER," +
                "all_reminders_completed BOOLEAN NOT NULL DEFAULT 0," +
                "is_valid BOOLEAN NOT NULL DEFAULT 1," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("数据库和表创建成功！");
        } catch (SQLException e) {
            System.err.println("创建数据库失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}