/**
 * Normalizes vertical / site / plant names for case- and whitespace-insensitive comparison.
 * @param {*} value
 * @returns {string}
 */
export const normalizeContextValue = (value) =>
  String(value ?? '')
    .trim()
    .toLowerCase()

const matchesField = (ruleValue, actualValue) => {
  if (ruleValue === undefined || ruleValue === null || ruleValue === '*') {
    return true
  }
  if (Array.isArray(ruleValue)) {
    return ruleValue.some(
      (item) => normalizeContextValue(item) === actualValue,
    )
  }
  return normalizeContextValue(ruleValue) === actualValue
}

const getRuleSpecificity = (match = {}) => {
  let score = 0
  if (match.vertical && match.vertical !== '*') score += 1
  if (match.site && match.site !== '*') score += 2
  if (match.plant && match.plant !== '*') score += 4
  return score
}

/**
 * Checks whether a single match specification applies to the given context.
 * @param {Object} match - { vertical?, site?, plant? } (string, string[], or '*')
 * @param {Object} context - { vertical, site, plant }
 * @returns {boolean}
 */
export const matchesPlantContext = (match = {}, context = {}) => {
  const vertical = normalizeContextValue(context.vertical)
  const site = normalizeContextValue(context.site)
  const plant = normalizeContextValue(context.plant)

  return (
    matchesField(match.vertical, vertical) &&
    matchesField(match.site, site) &&
    matchesField(match.plant, plant)
  )
}

/**
 * Resolves the highest-specificity matching rule for { vertical, site, plant }.
 * Precedence:
 *   1. Custom `when(context)` predicate (+8)
 *   2. Exact plant (+4)
 *   3. Exact site (+2)
 *   4. Exact vertical (+1)
 * Ties preserve declaration order.
 *
 * @param {Array} rules - List of variant rules ({ key, match, when?, Component?, config? })
 * @param {Object} context - Normalized plant context from usePlantContext()
 * @param {Object|null} fallback - Fallback entry if no rule matches
 * @returns {Object|null}
 */
export const resolveByPlantContext = (
  rules = [],
  context = {},
  fallback = null,
) => {
  if (!Array.isArray(rules) || rules.length === 0) {
    return fallback
  }

  const matched = rules
    .map((rule, index) => ({
      rule,
      index,
      score:
        getRuleSpecificity(rule?.match) +
        (typeof rule?.when === 'function' ? 8 : 0),
    }))
    .filter(({ rule }) => {
      if (!rule) return false
      if (!matchesPlantContext(rule.match || {}, context)) return false
      return typeof rule.when === 'function' ? Boolean(rule.when(context)) : true
    })
    .sort((a, b) => b.score - a.score || a.index - b.index)

  return matched.length > 0 ? matched[0].rule : fallback
}

