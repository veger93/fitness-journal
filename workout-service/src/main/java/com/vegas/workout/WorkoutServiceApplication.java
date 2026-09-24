package com.vegas.workout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * workout-service: Каталог упражнений (+ мышцы, медиа для анимаций), свои упражнения, комплексы, тренировки, подходы.
 */
@SpringBootApplication
public class WorkoutServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkoutServiceApplication.class, args);
    }
}
