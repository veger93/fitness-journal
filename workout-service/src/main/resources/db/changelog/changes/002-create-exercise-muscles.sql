--liquibase formatted sql

--changeset sergey:002-create-exercise-muscles
-- Какие мышцы работают в упражнении. PRIMARY — подсвечиваем ярко, SECONDARY — бледнее.
CREATE TABLE exercise_muscles (
    exercise_id UUID        NOT NULL,
    muscle      VARCHAR(30) NOT NULL,
    role        VARCHAR(10) NOT NULL,

    CONSTRAINT pk_exercise_muscles          PRIMARY KEY (exercise_id, muscle),  -- мышца в упражнении один раз
    CONSTRAINT fk_exercise_muscles_exercise FOREIGN KEY (exercise_id) REFERENCES exercises (id) ON DELETE CASCADE,
    CONSTRAINT ck_exercise_muscles_role     CHECK (role IN ('PRIMARY', 'SECONDARY')),
    CONSTRAINT ck_exercise_muscles_muscle   CHECK (muscle IN ('CHEST', 'FRONT_DELTS', 'SIDE_DELTS', 'REAR_DELTS', 'BICEPS', 'TRICEPS', 'FOREARMS', 'ABS', 'OBLIQUES', 'UPPER_BACK', 'LATS', 'LOWER_BACK', 'TRAPS', 'GLUTES', 'QUADS', 'HAMSTRINGS', 'ADDUCTORS', 'CALVES'))
);
--rollback DROP TABLE exercise_muscles;
