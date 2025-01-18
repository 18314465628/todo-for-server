@echo off
chcp 65001 > nul

:: 创建 UTF-8 编码的文件
echo 待办事项提醒程序使用说明 > readme_template.txt
echo. >> readme_template.txt
echo 1. 运行方法 >> readme_template.txt
echo 直接双击 todo-reminder.exe 即可运行程序 >> readme_template.txt
echo. >> readme_template.txt
echo 2. 数据库结构说明 >> readme_template.txt
echo 数据库文件：todo.db (SQLite数据库) >> readme_template.txt
echo 表名：todo_items >> readme_template.txt
echo 字段说明： >> readme_template.txt
echo - id: 自增主键 >> readme_template.txt
echo - user_id: 用户ID（整数） >> readme_template.txt
echo - task: 提醒内容 >> readme_template.txt
echo - password: Server酱密钥 >> readme_template.txt
echo - api: 接口地址 >> readme_template.txt
echo - reminder_time_1: 第一次提醒时间（必填，格式：yyyy-MM-dd HH:mm:ss） >> readme_template.txt
echo - reminder_time_2: 第二次提醒时间（可选） >> readme_template.txt
echo - reminder_time_3: 第三次提醒时间（可选） >> readme_template.txt
echo - execution_status_1: 第一次提醒执行状态 >> readme_template.txt
echo - execution_status_2: 第二次提醒执行状态 >> readme_template.txt
echo - execution_status_3: 第三次提醒执行状态 >> readme_template.txt
echo - api_response_1: 第一次提醒接口返回 >> readme_template.txt
echo - api_response_2: 第二次提醒接口返回 >> readme_template.txt
echo - api_response_3: 第三次提醒接口返回 >> readme_template.txt
echo - all_reminders_completed: 是否所有提醒已完成 >> readme_template.txt
echo - is_valid: 记录是否有效 >> readme_template.txt
echo - created_at: 创建时间 >> readme_template.txt
echo - updated_at: 更新时间 >> readme_template.txt
echo. >> readme_template.txt
echo 3. 使用步骤 >> readme_template.txt
echo a^) 获取Server酱密钥 >> readme_template.txt
echo    - 访问 https://sct.ftqq.com/ >> readme_template.txt
echo    - 登录并获取密钥（SendKey） >> readme_template.txt
echo. >> readme_template.txt
echo b^) 添加提醒任务 >> readme_template.txt
echo    - 在数据库中插入记录，必填字段： >> readme_template.txt
echo      * user_id: 用户ID >> readme_template.txt
echo      * task: 提醒内容 >> readme_template.txt
echo      * password: Server酱密钥 >> readme_template.txt
echo      * api: 接口地址（通常为：https://sctapi.ftqq.com/） >> readme_template.txt
echo      * reminder_time_1: 第一次提醒时间 >> readme_template.txt
echo      * execution_status_1: 初始状态（可设为null） >> readme_template.txt
echo. >> readme_template.txt
echo c^) 运行程序 >> readme_template.txt
echo    - 程序会自动检查提醒时间 >> readme_template.txt
echo    - 到达提醒时间时自动发送消息 >> readme_template.txt
echo    - 发送失败会在5分钟内重试（最多3次） >> readme_template.txt
echo. >> readme_template.txt
echo 4. 注意事项 >> readme_template.txt
echo - 程序会自动创建数据库文件 >> readme_template.txt
echo - 数据库文件 (todo.db) 会保存在程序所在目录 >> readme_template.txt
echo - 请不要删除 jre 和 lib 目录 >> readme_template.txt
echo - 时间格式必须为：yyyy-MM-dd HH:mm:ss >> readme_template.txt
echo - 程序会每分钟检查一次提醒时间 >> readme_template.txt
echo - 提醒状态说明： >> readme_template.txt
echo   * null: 未执行 >> readme_template.txt
echo   * success: 发送成功 >> readme_template.txt
echo   * failed: 发送失败 >> readme_template.txt
echo. >> readme_template.txt
echo 5. 示例SQL >> readme_template.txt
echo -- 添加提醒任务 >> readme_template.txt
echo INSERT INTO todo_items ( >> readme_template.txt
echo     user_id,  >> readme_template.txt
echo     task,  >> readme_template.txt
echo     password,  >> readme_template.txt
echo     api,  >> readme_template.txt
echo     reminder_time_1, >> readme_template.txt
echo     execution_status_1 >> readme_template.txt
echo ^) VALUES ( >> readme_template.txt
echo     1, >> readme_template.txt
echo     '这是一个提醒内容', >> readme_template.txt
echo     'SCTxxxxx', -- 替换为你的Server酱密钥 >> readme_template.txt
echo     'https://sctapi.ftqq.com/', >> readme_template.txt
echo     '2025-01-20 10:00:00', >> readme_template.txt
echo     null >> readme_template.txt
echo ^); >> readme_template.txt

echo readme_template.txt 文件已创建！
pause 