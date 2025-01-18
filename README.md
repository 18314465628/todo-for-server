# todo-for-server
待办事项，通过server酱的免费提醒通知服务，将你需要完成的事情，以微信服务的通知来提醒你。
1. 运行方法 
直接双击 todo-reminder.exe 即可运行程序 
 
2. 数据库结构说明 
数据库文件：todo.db (SQLite数据库) 
表名：todo_items 
字段说明： 
- id: 自增主键 
- user_id: 用户ID（整数） 
- task: 提醒内容 
- password: Server酱密钥 
- api: 接口地址 
- reminder_time_1: 第一次提醒时间（必填，格式：yyyy-MM-dd HH:mm:ss） 
- reminder_time_2: 第二次提醒时间（可选） 
- reminder_time_3: 第三次提醒时间（可选） 
- execution_status_1: 第一次提醒执行状态 
- execution_status_2: 第二次提醒执行状态 
- execution_status_3: 第三次提醒执行状态 
- api_response_1: 第一次提醒接口返回 
- api_response_2: 第二次提醒接口返回 
- api_response_3: 第三次提醒接口返回 
- all_reminders_completed: 是否所有提醒已完成 
- is_valid: 记录是否有效 
- created_at: 创建时间 
- updated_at: 更新时间 
 
3. 使用步骤 
a) 获取Server酱密钥 
   - 访问 https://sct.ftqq.com/ 
   - 登录并获取密钥（SendKey） 
 
b) 添加提醒任务 
   - 在数据库中插入记录，必填字段： 
     * user_id: 用户ID 
     * task: 提醒内容 
     * password: Server酱密钥 
     * api: 接口地址（通常为：https://sctapi.ftqq.com/） 
     * reminder_time_1: 第一次提醒时间 
     * execution_status_1: 初始状态（可设为null） 
 
c) 运行程序 
   - 程序会自动检查提醒时间 
   - 到达提醒时间时自动发送消息 
   - 发送失败会在5分钟内重试（最多3次） 
 
4. 注意事项 
- 程序会自动创建数据库文件 
- 数据库文件 (todo.db) 会保存在程序所在目录 
- 请不要删除 jre 和 lib 目录 
- 时间格式必须为：yyyy-MM-dd HH:mm:ss 
- 程序会每分钟检查一次提醒时间 
- 提醒状态说明： 
  * null: 未执行 
  * success: 发送成功 
  * failed: 发送失败 
 
5. 示例SQL 
-- 添加提醒任务 
INSERT INTO todo_items ( 
    user_id,  
    task,  
    password,  
    api,  
    reminder_time_1, 
    execution_status_1 
) VALUES ( 
    1, 
    '这是一个提醒内容', 
    'SCTxxxxx', -- 替换为你的Server酱密钥 
    'https://sctapi.ftqq.com/', 
    '2025-01-20 10:00:00', 
    null 
); 
6. 你还在为这些事情发愁吗？
   - 忘记女友纪念日，结果狂发 100 条消息道歉
   - 错过了双十一特价，眼睁睁看着心仪商品涨价
   - 忘记交水电费，回家发现断电断网
   - 错过了限时游戏活动，白白浪费体力值
   - 忘记给老板回复邮件，第二天尴尬面对
   - 忘记给宠物喂食，结果它用幽怨的眼神看着你
   - 忘记了今天要上课，梦游一样冲向教室
   - 忘记了今天要开会，被同事在群里疯狂@

7. Todo Reminder 来帮你！
   - 精准提醒：到点就通知，绝不让你错过
   - 多重保障：最多设置三次提醒，防止你真的睡过头
   - 失败重试：发送失败自动重试，除非你把手机也弄丢了
   - 永久保存：所有提醒记录永久保存，方便回顾那些年错过的事
   - 简单易用：复制粘贴 SQL 就能添加提醒，程序员专属浪漫
   - 安全可靠：基于 Server 酱推送，微信提醒贴心又安全
   - 离线运行：不联网也能运行，断网也不耽误提醒
   - 资源占用低：躲在后台默默工作，从不打扰你

8. 还在等什么？
   赶快试试 Todo Reminder，让生活不再手忙脚乱！
   你的贴心助手，你的提醒管家，你的记忆备份！
