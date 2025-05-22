// Script.js - Common JavaScript functions for Loc Auto application

document.addEventListener('DOMContentLoaded', function() {
    // Add Bootstrap validation
    enableFormValidation();
    
    // Initialize tooltips
    initTooltips();
    
    // Auto-dismiss alerts after 5 seconds
    setupAlertDismiss();
    
    // Add animation to cards
    animateCards();
});

/**
 * Enable Bootstrap form validation
 */
function enableFormValidation() {
    // Fetch all forms that need validation
    const forms = document.querySelectorAll('.needs-validation');
    
    // Loop over them and prevent submission
    Array.from(forms).forEach(form => {
        form.addEventListener('submit', event => {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            
            form.classList.add('was-validated');
        }, false);
    });
}

/**
 * Initialize Bootstrap tooltips
 */
function initTooltips() {
    const tooltipTriggerList = Array.from(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
}

/**
 * Setup automatic dismissal of alerts after 5 seconds
 */
function setupAlertDismiss() {
    const alerts = document.querySelectorAll('.alert:not(.alert-permanent)');
    
    alerts.forEach(alert => {
        setTimeout(() => {
            if (alert) {
                const bsAlert = new bootstrap.Alert(alert);
                bsAlert.close();
            }
        }, 5000);
    });
}

/**
 * Add fade-in animation to cards
 */
function animateCards() {
    const cards = document.querySelectorAll('.card');
    
    cards.forEach((card, index) => {
        card.classList.add('fadeIn');
        card.style.animationDelay = `${index * 0.1}s`;
    });
}

/**
 * Format a number as currency
 * @param {number} amount - The amount to format
 * @returns {string} Formatted currency string
 */
function formatCurrency(amount) {
    return new Intl.NumberFormat('fr-FR', { 
        style: 'currency', 
        currency: 'EUR' 
    }).format(amount);
}

/**
 * Format a date as dd/mm/yyyy
 * @param {Date} date - The date to format
 * @returns {string} Formatted date string
 */
function formatDate(date) {
    if (!(date instanceof Date)) {
        date = new Date(date);
    }
    
    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();
    
    return `${day}/${month}/${year}`;
}

/**
 * Calculate rental end date
 * @param {Date} startDate - The start date
 * @param {number} duration - The duration in days
 * @returns {Date} The end date
 */
function calculateEndDate(startDate, duration) {
    if (!(startDate instanceof Date)) {
        startDate = new Date(startDate);
    }
    
    const endDate = new Date(startDate);
    endDate.setDate(endDate.getDate() + duration);
    
    return endDate;
}

/**
 * Calculate total rental price
 * @param {number} pricePerDay - The price per day
 * @param {number} duration - The duration in days
 * @returns {number} The total price
 */
function calculateTotalPrice(pricePerDay, duration) {
    return pricePerDay * duration;
} 