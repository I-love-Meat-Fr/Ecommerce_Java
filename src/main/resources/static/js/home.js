/* ============================================================
   HOME PAGE - Interactive JavaScript
   Counter, tabs, countdown, activity feed, FAQ, carousel
   ============================================================ */

(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {
        initScrollReveal();
        initCountUp();
        initTabs();
        initFlashCountdown();
        initActivityFeed();
        initFAQ();
        initMarqueePause();
        initLoadMore();
        initQuickView();
        initNewsletter();
        initNavbar();
    });

    /* ---------- Navbar: Scroll effect + Dropdown + Mobile Menu ---------- */
    function initNavbar() {
        // Scroll shadow
        const header = document.querySelector('.home-header');
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

        // User dropdown toggle
        const userBtn = document.getElementById('user-menu-btn');
        const userMenu = document.querySelector('.home-user-menu');
        if (userBtn && userMenu) {
            userBtn.addEventListener('click', function(e) {
                e.stopPropagation();
                userMenu.classList.toggle('open');
            });

            document.addEventListener('click', function(e) {
                if (!userMenu.contains(e.target)) {
                    userMenu.classList.remove('open');
                }
            });

            document.addEventListener('keydown', function(e) {
                if (e.key === 'Escape') {
                    userMenu.classList.remove('open');
                }
            });
        }

        // Mobile menu toggle
        const hamburger = document.getElementById('home-hamburger-btn');
        const mobileMenu = document.getElementById('home-mobile-menu');
        const mobileClose = document.getElementById('home-mobile-close');

        if (hamburger && mobileMenu) {
            hamburger.addEventListener('click', function() {
                mobileMenu.classList.add('open');
                document.body.style.overflow = 'hidden';
            });

            if (mobileClose) {
                mobileClose.addEventListener('click', function() {
                    mobileMenu.classList.remove('open');
                    document.body.style.overflow = '';
                });
            }

            // Close on ESC
            document.addEventListener('keydown', function(e) {
                if (e.key === 'Escape' && mobileMenu.classList.contains('open')) {
                    mobileMenu.classList.remove('open');
                    document.body.style.overflow = '';
                }
            });
        }
    }

    /* ---------- Scroll Reveal ---------- */
    function initScrollReveal() {
        const reveals = document.querySelectorAll('.reveal');
        if (!reveals.length) return;

        const observer = new IntersectionObserver(function(entries) {
            entries.forEach(function(entry) {
                if (entry.isIntersecting) {
                    entry.target.classList.add('visible');
                    observer.unobserve(entry.target);
                }
            });
        }, { rootMargin: '0px 0px -40px 0px', threshold: 0.1 });

        reveals.forEach(function(el) { observer.observe(el); });
    }

    /* ---------- Count-up Animation ---------- */
    function initCountUp() {
        const counters = document.querySelectorAll('[data-count]');
        if (!counters.length) return;

        const observer = new IntersectionObserver(function(entries) {
            entries.forEach(function(entry) {
                if (entry.isIntersecting && !entry.target.classList.contains('counted')) {
                    entry.target.classList.add('counted');
                    animateCount(entry.target);
                }
            });
        }, { threshold: 0.5 });

        counters.forEach(function(counter) { observer.observe(counter); });
    }

    function animateCount(el) {
        const target = parseInt(el.getAttribute('data-count'), 10);
        const suffix = el.getAttribute('data-suffix') || '';
        const prefix = el.getAttribute('data-prefix') || '';
        const duration = 1200;
        const start = performance.now();

        function update(now) {
            const elapsed = now - start;
            const progress = Math.min(elapsed / duration, 1);
            // Ease out cubic
            const eased = 1 - Math.pow(1 - progress, 3);
            const current = Math.round(eased * target);

            el.textContent = prefix + formatNumber(current) + suffix;

            if (progress < 1) {
                requestAnimationFrame(update);
            } else {
                el.textContent = prefix + formatNumber(target) + suffix;
            }
        }

        requestAnimationFrame(update);
    }

    function formatNumber(num) {
        return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
    }

    /* ---------- Tab Switcher ---------- */
    function initTabs() {
        const tabBtns = document.querySelectorAll('.tab-btn');
        const tabPanels = document.querySelectorAll('.tab-panel');

        if (!tabBtns.length) return;

        tabBtns.forEach(function(btn) {
            btn.addEventListener('click', function() {
                const target = this.getAttribute('data-tab');

                tabBtns.forEach(function(b) { b.classList.remove('active'); });
                this.classList.add('active');

                tabPanels.forEach(function(panel) {
                    panel.classList.remove('active');
                    if (panel.getAttribute('data-panel') === target) {
                        panel.classList.add('active');
                    }
                });

                // Re-trigger count-up for newly visible cards
                const visiblePanel = document.querySelector('.tab-panel.active');
                if (visiblePanel) {
                    const newCounters = visiblePanel.querySelectorAll('[data-count]');
                    newCounters.forEach(function(c) {
                        if (!c.classList.contains('counted')) {
                            c.classList.add('counted');
                            animateCount(c);
                        }
                    });
                }
            });
        });
    }

    /* ---------- Flash Deal Countdown ---------- */
    function initFlashCountdown() {
        const cdEl = document.querySelector('.cd-number');
        if (!cdEl) return;

        // Set end time: 3 hours from now
        let endTime = localStorage.getItem('flash_end_time');
        if (!endTime || parseInt(endTime) < Date.now()) {
            endTime = Date.now() + 3 * 60 * 60 * 1000;
            localStorage.setItem('flash_end_time', endTime);
        } else {
            endTime = parseInt(endTime);
        }

        function tick() {
            const now = Date.now();
            const diff = endTime - now;

            if (diff <= 0) {
                document.querySelectorAll('.cd-number').forEach(function(el) {
                    el.textContent = '00';
                });
                return;
            }

            const totalSeconds = Math.floor(diff / 1000);
            const hours = Math.floor(totalSeconds / 3600);
            const minutes = Math.floor((totalSeconds % 3600) / 60);
            const seconds = totalSeconds % 60;

            const nums = document.querySelectorAll('.cd-number');
            if (nums.length >= 3) {
                nums[0].textContent = pad(hours);
                nums[1].textContent = pad(minutes);
                nums[2].textContent = pad(seconds);
            }

            setTimeout(tick, 1000);
        }

        tick();
    }

    function pad(n) {
        return n < 10 ? '0' + n : String(n);
    }

    /* ---------- Activity Feed ---------- */
    function initActivityFeed() {
        const mainList = document.querySelector('.activity-list');
        const sidePanel = document.querySelector('.activity-side-panel');
        if (!mainList && !sidePanel) return;

        const activities = [
            { icon: 'fa-shopping-bag', text: '<strong>Nguyễn Minh</strong> vừa đặt <span class="teal">Áo Thununisex Classic</span>', time: 'vừa xong' },
            { icon: 'fa-store', text: '<strong>Shop TechZone</strong> vừa đăng 3 sản phẩm mới', time: '1 phút trước' },
            { icon: 'fa-star', text: '<strong>Hồng Loan</strong> đánh giá 5 sao cho <span class="teal">Giày Sneaker Pro</span>', time: '2 phút trước' },
            { icon: 'fa-bolt', text: '<strong>Shop Fashion House</strong> vừa giảm giá 40%', time: '3 phút trước' },
            { icon: 'fa-truck', text: '<strong>Đơn hàng #2341</strong> đang được vận chuyển đến Hà Nội', time: '4 phút trước' },
            { icon: 'fa-heart', text: '<strong>Lê Thu</strong> thêm <span class="teal">Túi Xách Da</span> vào yêu thích', time: '5 phút trước' },
            { icon: 'fa-user-plus', text: '<strong>Trần Đức</strong> vừa đăng ký thành công', time: '6 phút trước' },
            { icon: 'fa-tags', text: '<strong>Shop Gadget Pro</strong> áp dụng mã <span class="teal">SUMMER24</span>', time: '7 phút trước' },
            { icon: 'fa-check-circle', text: '<strong>Đơn hàng #2338</strong> đã được giao thành công', time: '8 phút trước' },
            { icon: 'fa-fire', text: '<strong>Áo Hoodie Zipper</strong> đang trending với 128 đơn', time: '9 phút trước' },
            { icon: 'fa-comments', text: '<strong>Shop BookCorner</strong> trả lời đánh giá 5 sao', time: '10 phút trước' },
            { icon: 'fa-percent', text: '<strong>Flash Sale</strong> sập giá 50% - chỉ còn 2 tiếng!', time: '11 phút trước' },
        ];

        let actIndex = 0;

        function pushActivity() {
            const act = activities[actIndex % activities.length];
            actIndex++;

            if (mainList) {
                const item = createActivityItem(act);
                mainList.insertBefore(item, mainList.firstChild);
                // Keep max 8 items
                while (mainList.children.length > 8) {
                    mainList.removeChild(mainList.lastChild);
                }
            }

            if (sidePanel) {
                const item = createSideActivityItem(act);
                sidePanel.insertBefore(item, sidePanel.querySelector('.activity-side-item') ? sidePanel.querySelector('.activity-side-item') : sidePanel.lastElementChild);
                // Keep max 6 items
                const items = sidePanel.querySelectorAll('.activity-side-item');
                if (items.length > 6) {
                    items[items.length - 1].remove();
                }
            }

            // Schedule next push: random 3-7 seconds
            setTimeout(pushActivity, 3000 + Math.random() * 4000);
        }

        // Start after initial delay
        setTimeout(pushActivity, 2000);
    }

    function createActivityItem(act) {
        const div = document.createElement('div');
        div.className = 'activity-item';
        div.innerHTML =
            '<div class="activity-icon"><i class="fas ' + act.icon + '"></i></div>' +
            '<div class="activity-content">' +
                '<div class="activity-text">' + act.text + '</div>' +
                '<div class="activity-time">' + act.time + '</div>' +
            '</div>';
        return div;
    }

    function createSideActivityItem(act) {
        const div = document.createElement('div');
        div.className = 'activity-side-item';
        div.innerHTML =
            '<div class="activity-side-dot"></div>' +
            '<div>' +
                '<div class="activity-side-text">' + act.text + '</div>' +
                '<div class="activity-side-time">' + act.time + '</div>' +
            '</div>';
        return div;
    }

    /* ---------- FAQ Accordion ---------- */
    function initFAQ() {
        const questions = document.querySelectorAll('.faq-question');
        if (!questions.length) return;

        questions.forEach(function(q) {
            q.addEventListener('click', function() {
                const item = this.closest('.faq-item');
                const isOpen = item.classList.contains('open');

                // Close all
                document.querySelectorAll('.faq-item').forEach(function(i) {
                    i.classList.remove('open');
                });

                // Toggle current
                if (!isOpen) {
                    item.classList.add('open');
                }
            });
        });
    }

    /* ---------- Marquee Pause on Hover ---------- */
    function initMarqueePause() {
        const track = document.querySelector('.marquee-track');
        if (track) {
            const bar = document.querySelector('.marquee-bar');
            if (bar) {
                bar.addEventListener('mouseenter', function() {
                    track.style.animationPlayState = 'paused';
                });
                bar.addEventListener('mouseleave', function() {
                    track.style.animationPlayState = 'running';
                });
            }
        }
    }

    /* ---------- Load More ---------- */
    function initLoadMore() {
        const loadBtns = document.querySelectorAll('.load-more-btn');
        if (!loadBtns.length) return;

        loadBtns.forEach(function(btn) {
            btn.addEventListener('click', function() {
                const panel = this.closest('.tab-panel');
                if (!panel) return;

                const hiddenCards = panel.querySelectorAll('.product-card[data-hidden="true"]');
                let count = 0;
                hiddenCards.forEach(function(card) {
                    if (count < 6) {
                        card.style.display = 'block';
                        card.setAttribute('data-hidden', 'false');
                        count++;
                    }
                });

                // Hide button if no more hidden cards
                const remaining = panel.querySelectorAll('.product-card[data-hidden="true"]');
                if (!remaining.length) {
                    this.classList.add('hidden');
                }
            });
        });
    }

    /* ---------- Quick View Modal ---------- */
    function initQuickView() {
        const triggers = document.querySelectorAll('[data-quick-view]');
        if (!triggers.length) return;

        triggers.forEach(function(trigger) {
            trigger.addEventListener('click', function(e) {
                e.preventDefault();
                const target = this.getAttribute('data-quick-view');
                openQuickView(target);
            });
        });

        // Close on overlay click or ESC
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') closeQuickView();
        });
    }

    function openQuickView(productId) {
        const productCard = document.querySelector('[data-product-id="' + productId + '"]');
        if (!productCard) return;

        const name = productCard.querySelector('.product-card-name')?.textContent || '';
        const price = productCard.querySelector('.product-card-price')?.textContent || '';
        const img = productCard.querySelector('.product-card-img')?.src || '';

        // Create overlay
        const overlay = document.createElement('div');
        overlay.id = 'qv-overlay';
        overlay.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,0.6);z-index:9998;display:flex;align-items:center;justify-content:center;padding:20px;backdrop-filter:blur(4px);';
        overlay.innerHTML =
            '<div style="background:#fff;max-width:480px;width:100%;border:2px solid var(--teal,#0fb5b0);position:relative;animation:fadeSlideUp 0.3s ease;">' +
                '<button onclick="this.closest(\'#qv-overlay\').remove()" style="position:absolute;top:12px;right:12px;background:none;border:none;font-size:20px;cursor:pointer;color:#94a3b8;z-index:1;">&times;</button>' +
                '<img src="' + img + '" style="width:100%;height:260px;object-fit:cover;background:#f7faf9;" onerror="this.style.display=\'none\'">' +
                '<div style="padding:20px;">' +
                    '<h3 style="font-size:18px;font-weight:800;color:#0b1320;margin-bottom:8px;">' + name + '</h3>' +
                    '<div style="font-size:22px;font-weight:900;color:#0fb5b0;margin-bottom:16px;">' + price + '</div>' +
                    '<a href="/products/' + productId + '" class="btn-teal" style="display:inline-flex;align-items:center;gap:8px;padding:12px 24px;background:#0fb5b0;color:#fff;border:2px solid #0fb5b0;font-size:14px;font-weight:700;border-radius:4px;text-decoration:none;transition:all 0.2s;">Xem chi tiết <i class="fas fa-arrow-right"></i></a>' +
                '</div>' +
            '</div>' +
            '<style>@keyframes fadeSlideUp{from{opacity:0;transform:translateY(20px)}to{opacity:1;transform:translateY(0)}}</style>';

        overlay.addEventListener('click', function(e) {
            if (e.target === overlay) overlay.remove();
        });

        document.body.appendChild(overlay);
    }

    /* ---------- Newsletter Form ---------- */
    function initNewsletter() {
        const form = document.querySelector('.newsletter-form');
        if (!form) return;

        form.addEventListener('submit', function(e) {
            e.preventDefault();
            const input = form.querySelector('input[type="email"]');
            if (!input) return;

            const email = input.value.trim();
            if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
                input.style.borderColor = '#ef4444';
                input.focus();
                return;
            }

            input.style.borderColor = '#22c55e';
            input.value = '';
            input.placeholder = 'Cảm ơn bạn đã đăng ký!';

            setTimeout(function() {
                input.placeholder = 'Nhập email của bạn...';
                input.style.borderColor = '';
            }, 3000);
        });
    }

})();
