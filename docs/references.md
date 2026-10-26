# Научная база "умного дневника"

Источники, на которые опираются формулы и эвристики аналитики.
Пополняется вместе с кодом: каждое правило в `analytics-service` ссылается сюда номером [N].

## Список литературы

1. **Epley B.** *Poundage Chart.* Boyd Epley Workout. Lincoln, NE: Body Enterprises, 1985.
   — формула расчётного 1ПМ: `1ПМ = вес × (1 + повторы / 30)`.

2. **LeSuer D.A., McCormick J.H., Mayhew J.L., Wasserstein R.L., Arnold M.D.**
   The accuracy of prediction equations for estimating 1-RM performance in the bench press, squat, and deadlift.
   *Journal of Strength and Conditioning Research*, 1997, 11(4): 211–213.
   — сравнение формул расчёта 1ПМ; точность падает при большом числе повторов (> 10).
   https://researchconnect.buffalo.edu/en/publications/accuracy-of-prediction-equations-for-determining-one-repetition-m/

3. **Zourdos M.C., Klemp A., Dolan C. et al.**
   Novel Resistance Training–Specific Rating of Perceived Exertion Scale Measuring Repetitions in Reserve.
   *Journal of Strength and Conditioning Research*, 2016, 30(1): 267–275.
   — шкала RPE по "повторам в запасе": RPE 10 = отказ, RPE 9 = 1 повтор в запасе.
   https://openrepository.aut.ac.nz/items/efef3b25-6701-4fb5-bb82-55fcd2a26027/full

4. **Bell L., Strafford B.W., Coleman M., Androulakis Korakakis P., Nolan D.**
   Integrating Deloading into Strength and Physique Sports Training Programmes: An International Delphi Consensus Approach.
   *Sports Medicine – Open*, 2023, 9: 87. https://doi.org/10.1186/s40798-023-00633-0
   — разгрузка обычно раз в 4–6 недель, ~7 дней, за счёт снижения объёма; интенсивность — по ситуации.

5. **Gabbett T.J.** The training–injury prevention paradox: should athletes be training smarter and harder?
   *British Journal of Sports Medicine*, 2016, 50(5): 273–280.
   — отношение острой (неделя) к хронической (4 недели) нагрузке (ACWR); резкие скачки нагрузки связаны с риском травм.
   https://pmc.ncbi.nlm.nih.gov/articles/PMC4789704

6. **Hulin B.T., Gabbett T.J.** Indeed association does not equal prediction: the never-ending search
   for the perfect acute:chronic workload ratio. *British Journal of Sports Medicine*, 2019, 53(3): 144–145.
   — ACWR — ассоциация, а не предсказание; порог 1.5 не "магическая граница", смотреть вместе с другими признаками.
   https://bjsm.bmj.com/content/53/3/144

7. **Latella C., Teo W.-P., Spathis J., van den Hoek D.** Long-Term Strength Adaptation: A 15-Year Analysis
   of Powerlifting Athletes. *Journal of Strength and Conditioning Research*, 2020.
   https://pmc.ncbi.nlm.nih.gov/articles/PMC7448836/
   — у тренированных атлетов сила растёт медленно (~10% за 1.5–2 года) и тем медленнее, чем выше уровень:
   самые слабые прибавляли примерно вдвое быстрее самых сильных ("эффект потолка").

8. **van den Hoek D.J., Beaumont P.L., van den Hoek A.K., Owen P.J., Garrett J.M., Buhmann R., Latella C.**
   Normative data for the squat, bench press and deadlift exercises in powerlifting: Data from 809,986 competition entries.
   *Journal of Science and Medicine in Sport*, 2024, 27(10): 734–742. https://pubmed.ncbi.nlm.nih.gov/39060209/
   — нормативы относительной силы (результат / вес тела) по полу и возрасту; например, 90-й перцентиль
   у мужчин 18–35 лет: присед 2.83, жим 1.95, становая 3.25 веса тела. Задел для будущих силовых стандартов.

9. **Bosquet L., Berryman N., Dupuy O., Mekary S., Arvisais D., Bherer L., Mujika I.**
   Effect of training cessation on muscular performance: A meta-analysis.
   *Scandinavian Journal of Medicine & Science in Sports*, 2013, 23(3): e140–e149. https://doi.org/10.1111/sms.12047
   — метаанализ 103 исследований: чем дольше перерыв, тем больше потери; максимальная сила
   у молодых практически не снижается примерно до 3–4 недель перерыва, у людей 65+ падает быстрее.

10. **Ogasawara R., Yasuda T., Ishii N., Abe T.**
    Comparison of muscle hypertrophy following 6-month of continuous and periodic strength training.
    *European Journal of Applied Physiology*, 2013, 113(4): 975–985.
    — перерывы по 3 недели давали небольшую просадку силы, которая возвращалась за 3 недели тренировок:
    итог за полгода не хуже, чем без перерывов.

11. **Seiler S.**
    What is best practice for training intensity and duration distribution in endurance athletes?
    *International Journal of Sports Physiology and Performance*, 2010, 5(3): 276–291. https://doi.org/10.1123/ijspp.5.3.276
    — у выносливостных спортсменов около 80% тренировок — низкой интенсивности, остальное — интенсивные;
    добавление интенсивной работы хорошо тренированным не давало убедительного выигрыша.

12. **Nielsen R.Ø., Parner E.T., Nohr E.A., Sørensen H., Lind M., Rasmussen S.**
    Excessive progression in weekly running distance and risk of running-related injuries: an association which varies
    according to type of injury. *Journal of Orthopaedic and Sports Physical Therapy*, 2014, 44(10): 739–747.
    https://doi.org/10.2519/jospt.2014.5164
    — 874 начинающих бегуна, год наблюдения: при росте километража больше 30% за две недели "дистанционных" травм
    (пателлофеморальная боль, ITBS и др.) было больше (HR 1.59, p = 0.07 — на грани значимости, исследование поисковое).
    Поэтому подсказка в приложении мягкая: "лучше прибавлять плавнее", без запугивания.

13. **Foster C., Porcari J.P., Ault S., Doro K., Dubiel J., Engen M., Kolman D., Xiong S.**
    Exercise prescription when there is no exercise test: The Talk Test. *Kinesiology*, 2018, 50(1): 33–48.
    https://hrcak.srce.hr/192696
    — "тест разговором": можно говорить спокойно / да, но с трудом / нельзя — соответствует интенсивности
    около вентиляционных порогов. Наши кнопки "Легко / Средне / Тяжело" — эти три ответа.

14. **Schoenfeld B.J., Ogborn D., Krieger J.W.**
    Effects of resistance training frequency on measures of muscle hypertrophy: a systematic review and meta-analysis.
    *Sports Medicine*, 2016, 46(11): 1689–1697. https://pubmed.ncbi.nlm.nih.gov/27102172/
    — тренировать группу мышц 2 раза в неделю выгоднее для роста, чем 1 раз.

15. **Grgic J., Schoenfeld B.J., Latella C.**
    Resistance training frequency and skeletal muscle hypertrophy: a review of available evidence.
    *Journal of Science and Medicine in Sport*, 2019, 22(3): 361–370. https://doi.org/10.1016/j.jsams.2018.09.223
    — при одинаковом недельном объёме частота почти не влияет на рост: важнее суммарный объём за неделю.
    Поэтому подсказка в плане мягкая: "2 раза в неделю обычно удобнее набрать объём", а не "1 раз — плохо".

16. **Ratamess N.A., Alvar B.A., Evetoch T.K., Housh T.J., Kibler W.B., Kraemer W.J., Triplett N.T.**
    (American College of Sports Medicine) Progression models in resistance training for healthy adults.
    *Medicine & Science in Sports & Exercise*, 2009, 41(3): 687–708. https://doi.org/10.1249/MSS.0b013e3181915670
    — частота: новичкам 2–3 тренировки всего тела в неделю, среднему уровню 3–4, опытным 4–5(6) со сплитом;
    неделя повторяется, нагрузку повышают постепенно.

> Перед публикацией описания проекта выходные данные стоит сверить с первоисточниками.

## Где что используется

| Правило в коде | Где | Источники |
|---|---|---|
| Расчётный 1ПМ по Эпли | `OneRepMax` | [1], [2] |
| Средний RPE, порог RPE ≥ 9 как признак усталости | `PerformanceCalculator`, `InsightsCalculator` | [3] |
| Риск перегрузки: недельный объём к среднему за 4 недели, пороги 1.3 / 1.5 | `InsightsCalculator.acuteChronicRatio` | [5], [6] |
| План разгрузки: 7 дней, объём вдвое меньше | `InsightsCalculator.deloadPlan` | [4] |
| Плато: 21 день без нового максимума при 3+ тренировках | `InsightsCalculator.plateau` | эвристика проекта (порог подбирается) |
| Разгрузка через 70% от 1ПМ | `InsightsCalculator.deloadPlan` | выбор проекта в рамках [4] |
| Эталонная кривая: темп роста силы падает с уровнем; продвинутый ~0.5% в месяц | `ExperienceLevel`, `ReferenceCurve` | [7] |
| Темп для новичка 3% и среднего уровня 1.5% в месяц | `ExperienceLevel` | эвристика проекта в духе [7] |
| Силовые стандарты (результат / вес тела) | — (план) | [8] |
| Письмо "давно не было тренировок": пара недель перерыва почти не влияет на силу, потерянное возвращается быстрее | `NotificationTemplates.inactivity` | [9], [10] |
| Порог перерыва по умолчанию 7 дней (настраивается 3–30) | `NotificationSettings` | эвристика проекта: напомнить раньше, чем начнутся потери по [9] |
| Бег: "Как далась пробежка?" — Легко / Средне / Тяжело (RPE 4 / 6 / 8) | `SetDialog` (фронт), `RunningCalculator.effort` | [13] |
| Подсказка "добавьте лёгких", если лёгких пробежек меньше половины | `RunningCalculator.effort` | порог 50% — эвристика проекта, мягче 80% из [11] (там элита) |
| Предупреждение о росте километража > 30% за 2 недели (база от 5 км) | `RunningCalculator.load` | [12]; минимальная база — эвристика проекта |
| Рекорд темпа — только на отрезках от 1 км | `PerformanceCalculator.bestPace` | эвристика проекта |
| План — повторяющаяся неделя: комплекс на день недели | `WeeklyPlanService` | [16] |
| Подсказка "группа мышц 1 раз в неделю — можно 2" | `PlanPage` (фронт) | [14], смягчено по [15] |
| Подсказка про дни отдыха при 7 тренировках в неделю | `PlanPage` (фронт) | [16] (до 5–6 дней у опытных) |
