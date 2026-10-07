/* ============================================================
   HOME PAGE - Core JS (above-the-fold)
   PERFORMANCE #7 — Code-split để script critical nhỏ (~8KB)
   load ngay, defer phần còn lại qua requestIdleCallback.

   Chứa: banner slider, navbar, scroll reveal, tabs, marquee pause.
   ============================================================ */

(function() {
    'use strict';

    document.addEventListener('DOMContentLoaded', function() {
        initScrollReveal();
        initTabs();
        initMarqueePause();
        initNavbar();
        initBannerSlider();
    });

    /* ---------- Banner Slider (Hero) ---------- */
    function initBannerSlider() {
        const track = document.getElementById('bannerTrack');
        if (!track) return;

        const slides = track.querySelectorAll('.banner-slide');
        const dots = document.querySelectorAll('#bannerIndicators .banner-dot');
        const prevBtn = document.getElementById('bannerPrev');
        const nextBtn = document.getElementById('bannerNext');
        const progressBar = document.getElementById('bannerProgressBar');
        const total = slides.length;
        if (!total) return;

        let current = 0;
        const AUTO_MS = 5000;
        let timer = null;
        let progressStart = 0;
        let progressRAF = null;

        function goTo(index) {
            current = (index + total) % total;
            track.style.transform = 'translateX(-' + (current * 100) + '%)';
            dots.forEach(function(d, i) {
                if (i === current) {
                    d.classList.add('active');
                } else {
                    d.classList.remove('active');
                }
            });
            resetProgress();
        }

        function next() { goTo(current + 1); }
        function prev() { goTo(current - 1); }

        function resetProgress() {
            if (!progressBar) return;
            progressBar.style.transition = 'none';
            progressBar.style.width = '0%';
            progressStart = performance.now();
            if (progressRAF) cancelAnimationFrame(progressRAF);

            function step(now) {
                const elapsed = now - progressStart;
                const pct = Math.min(elapsed / AUTO_MS, 1);
                progressBar.style.width = (pct * 100) + '%';
                if (pct < 1) {
                    progressRAF = requestAnimationFrame(step);
                }
            }
            progressRAF = requestAnimationFrame(step);
        }

        function startAuto() {
            stopAuto();
            timer = setInterval(next, AUTO_MS);
            resetProgress();
        }

        function stopAuto() {
            if (timer) { clearInterval(timer); timer = null; }
            if (progressRAF) { cancelAnimationFrame(progressRAF); progressRAF = null; }
            if (progressBar) {
                progressBar.style.transition = 'none';
                progressBar.style.width = '0%';
            }
        }

        if (prevBtn) {
            prevBtn.addEventListener('click', function() {
                prev();
                startAuto();
            });
        }

        if (nextBtn) {
            nextBtn.addEventListener('click', function() {
                next();
                startAuto();
            });
        }

        dots.forEach(function(dot) {
            dot.addEventListener('click', function() {
                const idx = parseInt(this.getAttribute('data-index'), 10);
                if (!isNaN(idx)) {
                    goTo(idx);
                    startAuto();
                }
            });
        });

        // Pause on hover
        const slider = track.closest('.banner-slider');
        if (slider) {
            slider.addEventListener('mouseenter', function() {
                if (timer) {
                    clearInterval(timer);
                    timer = null;
                }
                if (progressRAF) { cancelAnimationFrame(progressRAF); progressRAF = null; }
            });
            slider.addEventListener('mouseleave', function() {
                startAuto();
            });
        }

        // Touch swipe
        let touchStartX = 0;
        let touchEndX = 0;
        track.addEventListener('touchstart', function(e) {
            touchStartX = e.changedTouches[0].screenX;
        }, { passive: true });
        track.addEventListener('touchend', function(e) {
            touchEndX = e.changedTouches[0].screenX;
            const diff = touchStartX - touchEndX;
            if (Math.abs(diff) > 50) {
                if (diff > 0) next();
                else prev();
                startAuto();
            }
        }, { passive: true });

        startAuto();
    }

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
        // Dropdown is bound by main.js (the global handler). Skip here to avoid
        // double-binding which would toggle the menu twice per click and appear stuck.
        // If for any reason main.js has not bound yet (e.g. it failed to load),
        // fall back to binding so the menu still works on the home page.
        if (userBtn && userMenu && !userBtn.dataset.dropdownBound) {
            userBtn.dataset.dropdownBound = '1';
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
                if (visiblePanel && window.CNJ70 && typeof window.CNJ70.animateCount === 'function') {
                    const newCounters = visiblePanel.querySelectorAll('[data-count]');
                    newCounters.forEach(function(c) {
                        if (!c.classList.contains('counted')) {
                            c.classList.add('counted');
                            window.CNJ70.animateCount(c);
                        }
                    });
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

})();