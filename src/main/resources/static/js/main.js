// CNJ70 Ecommerce - Main JavaScript

document.addEventListener('DOMContentLoaded', function() {
    // #region agent log H11 init
    fetch('http://127.0.0.1:7880/ingest/c62a6c44-8a64-4ebc-b91a-ead24f484206',{method:'POST',headers:{'Content-Type':'application/json','X-Debug-Session-Id':'04f262'},body:JSON.stringify({sessionId:'04f262',location:'main.js:init',message:'main.js loaded',data:{url:location.pathname,stepperNodes:document.querySelectorAll('.qty-stepper').length,removeForms:document.querySelectorAll('.cart-item-remove-form').length},timestamp:Date.now(),hypothesisId:'H11'})}).catch(()=>{});
    // #endregion
    // ========== Header Scroll Effect ==========
    const header = document.querySelector('.header');
    if (header) {
        let lastScroll = 0;
        window.addEventListener('scroll', function() {
            const currentScroll = window.pageYOffset;
            if (currentScroll > 20) {
                header.classList.add('scrolled');
            } else {
                header.classList.remove('scrolled');
            }
            lastScroll = currentScroll;
        }, { passive: true });
    }

    // ========== Scroll Animations ==========
    const animatedElements = document.querySelectorAll('[data-animate]');
    if (animatedElements.length > 0) {
        const observerOptions = {
            root: null,
            rootMargin: '0px 0px -60px 0px',
            threshold: 0.1
        };

        const observer = new IntersectionObserver(function(entries) {
            entries.forEach(function(entry) {
                if (entry.isIntersecting) {
                    const delay = entry.target.getAttribute('data-animate-delay') || 0;
                    setTimeout(function() {
                        entry.target.classList.add('animate-visible');
                    }, parseInt(delay));
                    observer.unobserve(entry.target);
                }
            });
        }, observerOptions);

        animatedElements.forEach(function(el) {
            el.classList.add('animate-hidden');
            observer.observe(el);
        });
    }

    // ========== Sidebar Toggle for Mobile ==========
    const sidebarToggle = document.querySelector('.sidebar-toggle');
    const sidebar = document.querySelector('.sidebar');

    if (sidebarToggle && sidebar) {
        sidebarToggle.addEventListener('click', function() {
            sidebar.classList.toggle('collapsed');
        });
    }

    // ========== Confirm Delete ==========
    const deleteForms = document.querySelectorAll('form[onsubmit*="confirm"]');
    deleteForms.forEach(form => {
        form.addEventListener('submit', function(e) {
            if (!confirm('Bạn có chắc chắn muốn thực hiện hành động này?')) {
                e.preventDefault();
            }
        });
    });

    // ========== Auto-hide alerts after 5 seconds ==========
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(alert => {
        setTimeout(() => {
            alert.style.transition = 'opacity 0.5s ease, transform 0.5s ease';
            alert.style.opacity = '0';
            alert.style.transform = 'translateY(-10px)';
            setTimeout(() => alert.remove(), 500);
        }, 5000);
    });

    // ========== Quantity selector validation ==========
    const quantityInputs = document.querySelectorAll('input[type="number"][name="quantity"]');
    quantityInputs.forEach(input => {
        input.addEventListener('change', function() {
            const min = parseInt(this.min) || 1;
            const max = parseInt(this.max) || 999;
            let value = parseInt(this.value);

            if (value < min) this.value = min;
            if (value > max) this.value = max;
        });
    });

    // ========== Add to Cart - AJAX (No Redirect) ==========
    const addToCartForms = document.querySelectorAll('.add-to-cart-form');
    addToCartForms.forEach(form => {
        form.addEventListener('submit', function(e) {
            e.preventDefault();

            const submitBtn = form.querySelector('button[type="submit"]') || form.querySelector('button');
            const originalText = submitBtn.innerHTML;

            submitBtn.disabled = true;
            submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang thêm...';

            const formData = new FormData(form);

            fetch('/api/cart/add', {
                method: 'POST',
                body: formData,
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    showToast('success', data.message || 'Đã thêm sản phẩm vào giỏ hàng');
                    if (typeof data.itemCount !== 'undefined') {
                        updateCartBadge(data.itemCount);
                    }
                } else {
                    showToast('error', data.message || 'Có lỗi xảy ra');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                showToast('error', 'Có lỗi xảy ra. Vui lòng thử lại.');
            })
            .finally(() => {
                submitBtn.disabled = false;
                submitBtn.innerHTML = originalText;
            });
        });
    });

    // ========== Cart Stepper (POST form, server-side redirect) ==========
    document.querySelectorAll('.qty-stepper').forEach(function(stepper) {
        const form = stepper.querySelector('.qty-stepper-form');
        const input = stepper.querySelector('.qty-input');
        const target = stepper.querySelector('.qty-target');
        const dec = stepper.querySelector('.qty-decrement');
        const inc = stepper.querySelector('.qty-increment');
        const productId = stepper.dataset.productId || 'unknown';
        if (!form || !input || !target || !dec || !inc) return;

        const stock = parseInt(stepper.dataset.stock) || 999;
        let currentQty = parseInt(input.value) || 1;

        function logStepper(action, next, willSubmit) {
            fetch('http://127.0.0.1:7880/ingest/c62a6c44-8a64-4ebc-b91a-ead24f484206',{method:'POST',headers:{'Content-Type':'application/json','X-Debug-Session-Id':'04f262'},body:JSON.stringify({sessionId:'04f262',location:'main.js:stepper:'+action,message:'stepper '+action,data:{productId:productId,currentQty:currentQty,stock:stock,next:next,willSubmit:willSubmit},timestamp:Date.now(),hypothesisId:'H11'})}).catch(()=>{});
        }

        dec.addEventListener('click', function(e) {
            e.preventDefault();
            logStepper('dec:click', currentQty - 1, false);
            // If currently 1 -> confirm remove
            if (currentQty <= 1) {
                if (confirm('Bạn có chắc muốn xóa sản phẩm này khỏi giỏ hàng?')) {
                    const removeForm = document.querySelector('.cart-item-remove-form input[value="' + productId + '"]');
                    if (removeForm && removeForm.form) {
                        logStepper('dec:remove-confirm', 0, true);
                        removeForm.form.submit();
                    } else {
                        logStepper('dec:remove-form-missing', 0, false);
                    }
                } else {
                    logStepper('dec:remove-cancel', 0, false);
                }
                return;
            }
            const next = currentQty - 1;
            target.value = next;
            logStepper('dec:submit', next, true);
            form.submit();
        });

        inc.addEventListener('click', function(e) {
            e.preventDefault();
            const next = currentQty + 1;
            logStepper('inc:click', next, false);
            if (next > stock) {
                logStepper('inc:blocked-stock', next, false);
                return;
            }
            target.value = next;
            logStepper('inc:submit', next, true);
            form.submit();
        });
    });

    // ========== Toast Notification ==========
    function showToast(type, message) {
        const existingToasts = document.querySelectorAll('.cart-toast');
        existingToasts.forEach(t => t.remove());

        const toast = document.createElement('div');
        toast.className = `cart-toast cart-toast-${type}`;
        toast.innerHTML = `
            <i class="fas ${type === 'success' ? 'fa-check-circle' : 'fa-exclamation-circle'}"></i>
            <span>${message}</span>
        `;

        if (!document.getElementById('cart-toast-styles')) {
            const style = document.createElement('style');
            style.id = 'cart-toast-styles';
            style.textContent = `
                .cart-toast {
                    position: fixed;
                    bottom: 24px;
                    right: 24px;
                    display: flex;
                    align-items: center;
                    gap: 10px;
                    padding: 14px 20px;
                    border-radius: 8px;
                    font-size: 14px;
                    font-weight: 500;
                    z-index: 9999;
                    animation: slideInToast 0.3s ease;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.15);
                }
                .cart-toast-success {
                    background: #10b981;
                    color: white;
                }
                .cart-toast-error {
                    background: #ef4444;
                    color: white;
                }
                .cart-toast i {
                    font-size: 18px;
                }
                @keyframes slideInToast {
                    from {
                        opacity: 0;
                        transform: translateX(100px);
                    }
                    to {
                        opacity: 1;
                        transform: translateX(0);
                    }
                }
            `;
            document.head.appendChild(style);
        }

        document.body.appendChild(toast);

        setTimeout(() => {
            toast.style.animation = 'slideInToast 0.3s ease reverse';
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    }

    // ========== Cart Badge Sync ==========
    function updateCartBadge(count) {
        document.querySelectorAll('.nav-cart-badge').forEach(el => el.remove());
        if (typeof count !== 'number' || count <= 0) return;
        const targets = [
            document.querySelector('#nav-cart'),
            document.querySelector('#cart-btn'),
            document.querySelector('.home-mobile-nav-link[href$="/cart"]')
        ].filter(Boolean);
        targets.forEach(target => {
            if (target.querySelector('.nav-cart-badge')) return;
            const badge = document.createElement('span');
            badge.className = 'nav-cart-badge';
            badge.textContent = count;
            target.appendChild(badge);
        });
    }

    function refreshCartBadge() {
        fetch('/api/cart/count', { headers: { 'X-Requested-With': 'XMLHttpRequest' } })
            .then(r => r.ok ? r.json() : null)
            .then(data => { if (data) updateCartBadge(data.totalQuantity); })
            .catch(() => {});
    }

    // ========== Refresh cart badge on page load ==========
    if (document.body.classList.contains('cart-page') ||
        document.querySelector('#nav-cart, #cart-btn')) {
        refreshCartBadge();
    }
});