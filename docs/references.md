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
