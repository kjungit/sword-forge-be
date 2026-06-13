# Probability and Random Reward Notes

Checked on 2026-06-11.

## Sources Reviewed

- GEEvo: Game Economy Generation and Balancing with Evolutionary Algorithms
  - https://arxiv.org/abs/2404.18574
  - Applied as: keep balance tuning simulation-driven, with source/sink totals and percentile checks.
- Gacha Game: When Prospect Theory Meets Optimal Pricing
  - https://arxiv.org/abs/2208.03602
  - Applied as: use worst-case mitigation through a visible pity stack, but avoid pricing or monetization optimization.
- Exploring Dynamic Difficulty Adjustment in Videogames
  - https://arxiv.org/abs/2007.07220
  - Applied as: use bounded, stateful assistance after repeated failure instead of static-only rates.
- Effect of Input-output Randomness on Gameplay Satisfaction in Collectable Card Games
  - https://arxiv.org/abs/2107.08437
  - Applied as: keep the random outcome visible and auditable, and avoid hiding progression planning behind opaque randomness.
- Deceptively Framed Lotteries in Consumer Markets
  - https://arxiv.org/abs/2511.01597
  - Applied as: avoid obscured probability framing; expose base rate, adjusted rate, pity stack, and pity bonus in API responses.
- Video game loot boxes are psychologically akin to gambling
  - https://doi.org/10.1038/s41562-018-0360-1
  - Applied as: avoid pay-to-random mechanics in backend assumptions, and make chance mechanics inspectable through logs and API responses.
- Gaming the system: suboptimal compliance with loot box probability disclosure regulations in China
  - https://doi.org/10.1017/bpp.2021.23
  - Applied as: probability details should be easy to find in Swagger/API responses, not buried in unrelated docs.

## Implemented Decisions

- Enhancement success rates are returned as both base and adjusted rates.
- Pity state is stored per grade in save data.
- Pity increases on failure and adds a capped bonus to the same grade.
- The current pity tuning is +2 percentage points per stack, capped at +20 percentage points.
- Because failure rewards are a material source, required evolution materials are available before the grade-cap stage and are not heavily drained by repurchase costs.
- Same-grade enhancement success keeps pity, so long failure streak protection still helps the current grade journey.
- Evolution success resets the source grade pity stack.
- Enhancement logs store audit details with pity values, roll, threshold, and failure rewards.
- Grade upgrades require evolution, which makes progression rules less ambiguous than letting enhancement silently cross grade boundaries.
- Balance simulation reports completion rate, attempt percentiles, source totals, and sink totals.
- Enhancement probability disclosure is available through `GET /api/v1/probabilities/enhance/{weaponId}`.

## Future Candidate

- Add a hard-pity or craft guarantee only if simulation p90/p95 remains too high after content-side tuning.
