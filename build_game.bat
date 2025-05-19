@echo off
setlocal enabledelayedexpansion

echo ========================================
echo =   Сборка игры ROGAL в EXE-файл      =
echo ========================================
echo.

:: Проверка наличия необходимых компонентов
if not exist "gradlew.bat" (
    echo [ERROR] Файл gradlew.bat не найден в текущей директории.
    echo Пожалуйста, запустите этот скрипт из корневой директории проекта.
    goto :error
)

:: Очистка предыдущей сборки
echo [1/4] Очистка предыдущей сборки...
call gradlew clean
if %errorlevel% neq 0 (
    echo [WARN] Очистка завершилась с ошибками, но продолжаем сборку...
)

:: Компиляция проекта
echo.
echo [2/4] Компиляция проекта...
call gradlew :lwjgl3:build
if %errorlevel% neq 0 (
    echo [ERROR] Ошибка при компиляции проекта.
    goto :error
)

:: Создание EXE файла
echo.
echo [3/4] Создание EXE файла...
call gradlew :lwjgl3:createExe
if %errorlevel% neq 0 (
    echo [ERROR] Ошибка при создании EXE файла.
    goto :error
)

:: Копирование EXE файла в корневую директорию
echo.
echo [4/4] Копирование готового EXE файла...
if exist "lwjgl3\build\launch4j\rogal.exe" (
    copy "lwjgl3\build\launch4j\rogal.exe" "rogal.exe" > nul
    echo [INFO] EXE файл скопирован: rogal.exe
) else (
    echo [ERROR] EXE файл не был создан.
    goto :error
)

:: Создание архива с игрой
echo.
echo [5/5] Создание архива с игрой...
call gradlew :lwjgl3:construoPackage
if %errorlevel% neq 0 (
    echo [WARN] Не удалось создать архив с игрой, но EXE файл был создан успешно.
)

echo.
echo ========================================
echo =   Сборка успешно завершена!         =
echo =   Файл: rogal.exe                    =
echo ========================================
goto :eof

:error
echo.
echo ========================================
echo =   Сборка завершилась с ошибками     =
echo ========================================
exit /b 1
