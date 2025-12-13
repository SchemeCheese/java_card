/**
 * Pagination utility functions
 */

/**
 * Parse pagination parameters from query string
 * @param {Object} query - Express request query object
 * @param {Object} options - Options with defaults
 * @returns {Object} Pagination parameters
 */
const parsePagination = (query, options = {}) => {
    const {
        defaultPage = 1,
        defaultLimit = 20,
        maxLimit = 100,
        minLimit = 1
    } = options;

    const page = Math.max(1, parseInt(query.page) || defaultPage);
    const limit = Math.min(
        maxLimit,
        Math.max(minLimit, parseInt(query.limit) || defaultLimit)
    );
    const offset = (page - 1) * limit;

    return { page, limit, offset };
};

/**
 * Format paginated response
 * @param {Object} result - Sequelize findAndCountAll result
 * @param {Number} page - Current page
 * @param {Number} limit - Items per page
 * @param {Array} data - Data array (optional, if not provided uses result.rows)
 * @returns {Object} Formatted pagination response
 */
const formatPaginatedResponse = (result, page, limit, data = null) => {
    const total = result.count || 0;
    const items = data !== null ? data : (result.rows || []);
    const totalPages = Math.ceil(total / limit);

    return {
        success: true,
        pagination: {
            page,
            limit,
            total,
            totalPages,
            hasNext: page < totalPages,
            hasPrev: page > 1
        },
        data: items
    };
};

/**
 * Create pagination metadata
 * @param {Number} total - Total items
 * @param {Number} page - Current page
 * @param {Number} limit - Items per page
 * @returns {Object} Pagination metadata
 */
const createPaginationMeta = (total, page, limit) => {
    const totalPages = Math.ceil(total / limit);
    return {
        page,
        limit,
        total,
        totalPages,
        hasNext: page < totalPages,
        hasPrev: page > 1
    };
};

module.exports = {
    parsePagination,
    formatPaginatedResponse,
    createPaginationMeta
};


