package com.todo;

import okhttp3.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

public class ReminderTask {
    // 将 ReminderInfo 移到这里，作为内部类
    private static class ReminderInfo {
        int id;
        String time;
        int index;
        String status;

        ReminderInfo(int id, String time, int index, String status) {
            this.id = id;
            this.time = time;
            this.index = index;
            this.status = status;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ReminderInfo that = (ReminderInfo) o;
            return time != null && time.equals(that.time);
        }

        @Override
        public int hashCode() {
            return time != null ? time.hashCode() : 0;
        }
    }

    private static final String DB_URL = "jdbc:sqlite:todo.db";
    private static final OkHttpClient client = new OkHttpClient();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter logFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final int RETRY_INTERVAL_SECONDS = 5;  // 重试间隔5秒
    private static final int MAX_RETRIES = 3;            // 最大重试次数
    private Map<String, LocalDateTime> lastRequestTimes = new HashMap<>();  // 记录每个任务的最后请求时间

    private String getCurrentTime() {
        return LocalDateTime.now().format(logFormatter);
    }

    public void startMonitoring() {
        Timer timer = new Timer();
        log("定时任务启动，每分钟检查一次提醒...");
        
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                log("\n-------------------------------------------");
                log("开始新一轮检查");
                checkReminders();
                log("本轮检查完成");
                log("-------------------------------------------\n");
            }
        }, 0, 60000); // 每分钟检查一次
    }

    private void log(String message) {
        System.out.println("[" + getCurrentTime() + "] " + message);
    }

    private void logError(String message) {
        System.err.println("[" + getCurrentTime() + "] ERROR: " + message);
    }

    private void checkReminders() {
        String sql = "SELECT id, user_id, task, password, api, reminder_time_1, reminder_time_2, reminder_time_3, " +
                "execution_status_1, execution_status_2, execution_status_3 " +
                "FROM todo_items WHERE is_valid = 1 AND all_reminders_completed = 0";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            boolean hasRecords = false;
            while (rs.next()) {
                hasRecords = true;
                int id = rs.getInt("id");
                log("检查任务 [ID=" + id + "]:");
                log("  任务内容: " + rs.getString("task"));
                checkAndSendReminder(conn, rs);
            }
            
            if (!hasRecords) {
                log("没有找到需要提醒的任务");
            }
        } catch (SQLException e) {
            logError("检查提醒时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void checkAndSendReminder(Connection conn, ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String task = rs.getString("task");
        String apiKey = rs.getString("password");

        // 收集所有有效的提醒时间
        Set<ReminderInfo> reminders = new HashSet<>();
        for (int i = 1; i <= 3; i++) {
            String reminderTime = rs.getString("reminder_time_" + i);
            String status = rs.getString("execution_status_" + i);

            log(String.format("检查任务 ID=%d, 提醒%d: 时间=%s, 状态=%s", 
                id, i, reminderTime, status));

            if (reminderTime != null && !reminderTime.isEmpty()) {
                ReminderInfo info = new ReminderInfo(id, reminderTime, i, status);
                if (reminders.add(info)) {
                    log("  添加提醒时间点: " + reminderTime);
                } else {
                    log("  跳过重复的提醒时间点: " + reminderTime);
                }
            } else {
                log("  提醒" + i + ": 未设置提醒时间，跳过");
            }
        }

        // 处理每个唯一的提醒时间
        for (ReminderInfo info : reminders) {
            TimeCompareResult timeCompareResult = compareTime(info.time);
            log(String.format("  处理时间点 %s (提醒%d): 比较结果=%s", 
                info.time, info.index, timeCompareResult));

            boolean needRemind = false;
            String reason = "";

            switch (timeCompareResult) {
                case FUTURE:
                    // 还没到时间：不需要提醒
                    needRemind = false;
                    reason = "未到提醒时间";
                    break;
                case IN_RANGE:
                    // 在有效时间范围内（已到达或超过提醒时间，且在5分钟内）
                    // 如果状态不是 success，就需要提醒
                    if (info.status == null || 
                        info.status.equalsIgnoreCase("null") || 
                        info.status.equals("failed") ||
                        !info.status.equals("success")) {
                        
                        // 检查是否可以重试（避免频繁请求）
                        String requestKey = id + "_" + info.time;
                        LocalDateTime lastRequestTime = lastRequestTimes.get(requestKey);
                        LocalDateTime now = LocalDateTime.now();
                        
                        if (lastRequestTime == null || 
                            java.time.Duration.between(lastRequestTime, now).getSeconds() >= RETRY_INTERVAL_SECONDS) {
                            
                            // 检查重试次数
                            int retryCount = getRetryCount(conn, id, info.index);
                            if (retryCount < MAX_RETRIES) {
                                needRemind = true;
                                reason = retryCount == 0 ? 
                                        "首次提醒" : 
                                        String.format("第%d次重试", retryCount + 1);
                                lastRequestTimes.put(requestKey, now);
                            } else {
                                reason = "已达到最大重试次数";
                            }
                        } else {
                            long waitSeconds = RETRY_INTERVAL_SECONDS - 
                                java.time.Duration.between(lastRequestTime, now).getSeconds();
                            reason = String.format("需要等待%d秒后才能重试", waitSeconds);
                        }
                    } else {
                        reason = "提醒已成功发送";
                    }
                    break;
                case EXPIRED:
                    // 超过5分钟：不再提醒
                    needRemind = false;
                    reason = "已超过提醒有效期（5分钟）";
                    break;
            }

            if (needRemind) {
                log("  需要发送提醒: " + task);
                log("  原因: " + reason);
                
                boolean success = sendReminder(conn, apiKey, task, info);
                updateReminderStatus(conn, id, info.index, success);
                
                log("  提醒发送" + (success ? "成功" : "失败"));
                
                if (success) {
                    checkAllRemindersCompleted(conn, id);
                }
            } else {
                log("  无需发送提醒: " + reason);
            }
        }
    }

    private enum TimeCompareResult {
        FUTURE,     // 未来时间
        IN_RANGE,   // 在有效范围内
        EXPIRED     // 已过期
    }

    /**
     * 比较系统当前时间和提醒时间
     * @return 时间比较结果
     */
    private TimeCompareResult compareTime(String reminderTime) {
        try {
            LocalDateTime current = LocalDateTime.now();
            LocalDateTime reminder = LocalDateTime.parse(reminderTime, formatter);
            
            log(String.format("  时间比较: 当前=%s, 提醒=%s", 
                current.format(formatter), reminder.format(formatter)));
            
            // 计算时间差（分钟）
            long minutesDiff = java.time.Duration.between(reminder, current).toMinutes();
            log(String.format("  时间差: %d 分钟", minutesDiff));

            if (minutesDiff < 0) {
                // 还没到时间
                return TimeCompareResult.FUTURE;
            } else if (minutesDiff <= 5) {
                // 在5分钟有效期内
                return TimeCompareResult.IN_RANGE;
            } else {
                // 超过5分钟，已过期
                return TimeCompareResult.EXPIRED;
            }
        } catch (Exception e) {
            logError("时间比较出错: " + e.getMessage());
            e.printStackTrace();
            return TimeCompareResult.EXPIRED; // 出错时当作已过期处理
        }
    }

    private boolean sendReminder(Connection conn, String apiKey, String task, ReminderInfo info) {
        String url = "https://sctapi.ftqq.com/" + apiKey + ".send";
        log("    正在发送提醒:");
        log("      URL: " + url);
        log("      内容: " + task);
        
        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("title", "待办事项提醒")
                .add("desp", task)
                .add("channel", "9")
                .add("openid", "")
                .add("short", "")
                .add("dtmd", "1");

        Request request = new Request.Builder()
                .url(url)
                .post(formBuilder.build())
                .build();

        try {
            Response response = client.newCall(request).execute();
            String responseBody = response.body().string();
            log("      Server酱响应: " + responseBody);
            
            // 保存响应内容
            saveApiResponse(conn, info.id, info.index, responseBody);
            
            if (!response.isSuccessful()) {
                logError("      发送失败，响应码: " + response.code());
                return false;
            }
            
            if (responseBody.contains("\"code\":0")) {
                log("      消息发送成功");
                return true;
            } else {
                logError("      Server酱返回错误: " + responseBody);
                return false;
            }
            
        } catch (Exception e) {
            String errorMsg = "发送失败: " + e.getMessage();
            logError("      " + errorMsg);
            saveApiResponse(conn, info.id, info.index, errorMsg);
            e.printStackTrace();
            return false;
        }
    }

    // 保存 API 响应和更新重试次数
    private void saveApiResponse(Connection conn, int id, int reminderIndex, String response) {
        String sql = "UPDATE todo_items SET " +
                    "api_response_" + reminderIndex + " = ?, " +  // API响应内容
                    "retry_count_" + reminderIndex + " = COALESCE(retry_count_" + reminderIndex + ", 0) + 1, " +  // 重试次数
                    "updated_at = datetime('now') " +
                    "WHERE id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, response);  // 保存完整的 API 响应
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
            log(String.format("  保存API响应: ID=%d, 提醒%d, 响应=%s", id, reminderIndex, response));
        } catch (SQLException e) {
            logError("保存API响应失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateReminderStatus(Connection conn, int id, int reminderIndex, boolean success) {
        String status = success ? "success" : "failed";
        String sql = "UPDATE todo_items SET execution_status_" + reminderIndex + " = ?, " +
                    "updated_at = datetime('now') WHERE id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
            log(String.format("  更新状态: ID=%d, 提醒%d, 新状态=%s", id, reminderIndex, status));
        } catch (SQLException e) {
            logError("更新提醒状态失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void checkAllRemindersCompleted(Connection conn, int id) {
        String sql = "SELECT reminder_time_1, reminder_time_2, reminder_time_3, " +
                "execution_status_1, execution_status_2, execution_status_3 " +
                "FROM todo_items WHERE id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                boolean allCompleted = true;
                
                for (int i = 1; i <= 3; i++) {
                    String time = rs.getString("reminder_time_" + i);
                    String status = rs.getString("execution_status_" + i);
                    
                    if (time != null && !time.isEmpty() && 
                        (status == null || !status.equals("success"))) {
                        allCompleted = false;
                        break;
                    }
                }
                
                if (allCompleted) {
                    updateAllRemindersCompleted(conn, id);
                }
            }
        } catch (SQLException e) {
            logError("检查提醒完成状态失败: " + e.getMessage());
        }
    }

    private void updateAllRemindersCompleted(Connection conn, int id) {
        String sql = "UPDATE todo_items SET all_reminders_completed = 1 WHERE id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logError("更新提醒完成状态失败: " + e.getMessage());
        }
    }

    // 获取重试次数
    private int getRetryCount(Connection conn, int id, int reminderIndex) {
        String sql = "SELECT retry_count_" + reminderIndex + " FROM todo_items WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                Integer retryCount = rs.getInt("retry_count_" + reminderIndex);
                return rs.wasNull() ? 0 : retryCount;
            }
        } catch (Exception e) {
            logError("获取重试次数失败: " + e.getMessage());
        }
        return 0;
    }

    // 修改数据库创建语句，添加重试次数字段
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
                "api_response_1 TEXT," +        // API响应内容
                "api_response_2 TEXT," +
                "api_response_3 TEXT," +
                "retry_count_1 INTEGER DEFAULT 0," +  // 重试次数
                "retry_count_2 INTEGER DEFAULT 0," +
                "retry_count_3 INTEGER DEFAULT 0," +
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