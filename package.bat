@echo off
chcp 65001 > nul
echo 正在打包程序...

:: 1. 清理并编译
call mvn clean package

:: 2. 创建发布目录
if exist "todo-reminder" rd /s /q "todo-reminder"
mkdir todo-reminder
copy target\todo-reminder.exe todo-reminder\

:: 3. 复制精简版JRE
xcopy /E /I jre8-minimal todo-reminder\jre

:: 4. 复制说明文件
copy /Y "readme_template.txt" "todo-reminder\README.txt"
if errorlevel 1 (
    echo 错误：复制说明文件失败
    pause
    exit /b 1
)

:: 5. 打包成zip
powershell Compress-Archive -Path todo-reminder -DestinationPath todo-reminder.zip -Force

echo 打包完成！
pause 