package ru.myitschool.rogal.CustomHelpers.utils;

import com.badlogic.gdx.Gdx;
import java.nio.charset.StandardCharsets;

/**
 * Вспомогательный класс для логирования с поддержкой русских символов
 */
public class LogHelper {
    
    /**
     * Выводит информационное сообщение в лог
     * @param tag тег сообщения
     * @param message текст сообщения
     */
    public static void info(String tag, String message) {
        String encodedMessage = new String(message.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        Gdx.app.log(tag, encodedMessage);
    }
    
    /**
     * Выводит стандартное сообщение в лог (алиас для info)
     * @param tag тег сообщения
     * @param message текст сообщения
     */
    public static void log(String tag, String message) {
        info(tag, message);
    }
    
    /**
     * Выводит отладочное сообщение в лог
     * @param tag тег сообщения
     * @param message текст сообщения
     */
    public static void debug(String tag, String message) {
        String encodedMessage = new String(message.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        Gdx.app.debug(tag, encodedMessage);
    }
    
    /**
     * Выводит сообщение об ошибке в лог
     * @param tag тег сообщения
     * @param message текст сообщения
     */
    public static void error(String tag, String message) {
        String encodedMessage = new String(message.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        Gdx.app.error(tag, encodedMessage);
    }
    
    /**
     * Выводит сообщение об ошибке в лог с информацией об исключении
     * @param tag тег сообщения
     * @param message текст сообщения
     * @param exception исключение
     */
    public static void error(String tag, String message, Throwable exception) {
        String encodedMessage = new String(message.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        Gdx.app.error(tag, encodedMessage + ": " + exception.getMessage());
    }
} 