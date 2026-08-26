// CNJ70 Ecommerce - Main JavaScript

document.addEventListener('DOMContentLoaded', function() {
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
            
            // Disable button while processing
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

    // ========== Toast Notification ==========
    function showToast(type, message) {
        // Remove existing toasts
        const existingToasts = document.querySelectorAll('.cart-toast');
        existingToasts.forEach(t => t.remove());
        
        const toast = document.createElement('div');
        toast.className = `cart-toast cart-toast-${type}`;
        toast.innerHTML = `
            <i class="fas ${type === 'success' ? 'fa-check-circle' : 'fa-exclamation-circle'}"></i>
            <span>${message}</span>
        `;
        
        // Add styles if not exists
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
        
        // Auto remove after 3 seconds
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
            if (target.id === 'cart-btn' || target.classList.contains('home-icon-btn')) {
                target.appendChild(badge);
            } else if (target.classList.contains('home-mobile-nav-link')) {
                target.appendChild(badge);
            } else {
                target.appendChild(badge);
            }
        });
    }

    function refreshCartBadge() {
        fetch('/api/cart/count', { headers: { 'X-Requested-With': 'XMLHttpRequest' } })
            .then(r => r.ok ? r.json() : null)
            .then(data => { if (data) updateCartBadge(data.totalQuantity); })
            .catch(() => {});
    }

    // ========== Cart Page: Quantity Stepper & Remove ==========
    const steppers = document.querySelectorAll('.qty-stepper');
    if (steppers.length > 0) {
        const moneyFormatter = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' });
        let updateInFlight = null;

        function updateQuantity(productId, qty, stepper) {
            if (updateInFlight && updateInFlight.productId === productId) return;
            const stock = parseInt(stepper.dataset.stock) || 999;
            const oldQty = parseInt(stepper.querySelector('.qty-input').value);
            if (qty < 1 || qty > stock) {
                showToast('error', `Số lượng phải từ 1 đến ${stock}`);
                stepper.querySelector('.qty-input').value = oldQty;
                return;
            }
            if (qty === oldQty) return;

            updateInFlight = { productId };
            const formData = new FormData();
            formData.append('productId', productId);
            formData.append('quantity', qty);

            stepper.querySelectorAll('.qty-btn').forEach(b => b.disabled = true);

            fetch('/api/cart/update', {
                method: 'POST',
                body: formData,
                headers: { 'X-Requested-With': 'XMLHttpRequest' }
            })
            .then(r => r.json())
            .then(data => {
                if (!data.success) {
                    showToast('error', data.message || 'Không thể cập nhật');
                    return;
                }
                if (data.removed) {
                    const card = document.querySelector(`.cart-item-card[data-product-id="${productId}"]`);
                    if (card) {
                        card.classList.add('is-removing');
                        setTimeout(() => {
                            card.remove();
                            maybeShowEmptyState();
                        }, 250);
                    }
                } else {
                    stepper.querySelector('.qty-input').value = data.quantity;
                    const card = document.querySelector(`.cart-item-card[data-product-id="${productId}"]`);
                    if (card && data.subtotal) {
                        const sub = card.querySelector('.cart-item-subtotal-value');
                        if (sub) sub.textContent = moneyFormatter.format(data.subtotal);
                    }
                }
                updateCartTotals(data.cartTotal, data.totalQuantity);
                updateCartBadge(data.totalQuantity);
            })
            .catch(err => {
                console.error(err);
                showToast('error', 'Có lỗi xảy ra');
            })
            .finally(() => {
                stepper.querySelectorAll('.qty-btn').forEach(b => {
                    const input = stepper.querySelector('.qty-input');
                    b.disabled = parseInt(input.value) >= stock && b.classList.contains('qty-increment');
                });
                updateInFlight = null;
            });
        }

        function updateCartTotals(cartTotal, totalQuantity) {
            const subtotalEls = document.querySelectorAll('.summary-subtotal');
            const totalEls = document.querySelectorAll('.summary-total-amount');
            subtotalEls.forEach(el => {
                if (cartTotal !== undefined) el.textContent = moneyFormatter.format(cartTotal);
            });
            totalEls.forEach(el => {
                if (cartTotal !== undefined) el.textContent = moneyFormatter.format(cartTotal);
            });
        }

        function maybeShowEmptyState() {
            const remaining = document.querySelectorAll('.cart-item-card');
            if (remaining.length > 0) return;
            const layout = document.querySelector('.cart-layout');
            const heroLeft = document.querySelector('.cart-hero-left h1');
            const heroStats = document.querySelector('.cart-hero-right');
            if (heroLeft) heroLeft.innerHTML = 'Giỏ hàng trống';
            if (heroStats) heroStats.style.display = 'none';
            if (layout) {
                layout.style.opacity = '0';
                setTimeout(() => location.reload(), 400);
            }
        }

        steppers.forEach(stepper => {
            const input = stepper.querySelector('.qty-input');
            const dec = stepper.querySelector('.qty-decrement');
            const inc = stepper.querySelector('.qty-increment');
            const productId = stepper.dataset.productId;
            const stock = parseInt(stepper.dataset.stock) || 999;

            dec.addEventListener('click', () => {
                const v = parseInt(input.value) - 1;
                updateQuantity(productId, v, stepper);
            });
            inc.addEventListener('click', () => {
                const v = parseInt(input.value) + 1;
                if (v > stock) {
                    showToast('error', `Chỉ còn ${stock} sản phẩm trong kho`);
                    return;
                }
                updateQuantity(productId, v, stepper);
            });
        });
    }

    const removeBtns = document.querySelectorAll('.cart-item-remove');
    removeBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            const productId = this.dataset.productId;
            if (!confirm('Bạn có chắc muốn xóa sản phẩm này khỏi giỏ hàng?')) return;
            const card = document.querySelector(`.cart-item-card[data-product-id="${productId}"]`);
            if (card) card.classList.add('is-removing');
            const formData = new FormData();
            formData.append('productId', productId);
            fetch('/api/cart/remove', {
                method: 'POST',
                body: formData,
                headers: { 'X-Requested-With': 'XMLHttpRequest' }
            })
            .then(r => r.json())
            .then(data => {
                if (!data.success) {
                    if (card) card.classList.remove('is-removing');
                    showToast('error', data.message || 'Không thể xóa sản phẩm');
                    return;
                }
                setTimeout(() => {
                    if (card) card.remove();
                    if (data.empty) {
                        location.reload();
                    } else {
                        updateCartTotals(data.cartTotal, data.totalQuantity);
                        updateCartBadge(data.totalQuantity);
                    }
                }, 250);
            })
            .catch(err => {
                console.error(err);
                if (card) card.classList.remove('is-removing');
                showToast('error', 'Có lỗi xảy ra');
            });
        });
    });

    // ========== Refresh cart badge on page load ==========
    if (document.body.classList.contains('cart-page') ||
        document.querySelector('#nav-cart, #cart-btn')) {
        refreshCartBadge();
    }
});
