/* ============================================================
   HOME PAGE - Flash Sale (deferred via requestIdleCallback)
   PERFORMANCE #7 — Tách countdown + marquee khỏi core JS.
   Flash sale section nằm giữa trang → không cần load ngay khi
   first-paint, idle-callback là đủ.

   Chứa: countdown, flash-sale marquee recycle logic.
   ============================================================ */

(function() {
    'use strict';

    function initFlashSale() {
        initFlashCountdown();
        initFlashMarquee();
    }

    if (document.readyState === 'complete') {
        initFlashSale();
    } else {
        // PERFORMANCE #7 — Đợi browser rảnh rỗi rồi mới khởi tạo.
        if ('requestIdleCallback' in window) {
            requestIdleCallback(initFlashSale, { timeout: 1500 });
        } else {
            setTimeout(initFlashSale, 800);
        }
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

    /* ---------- Flash-sale marquee: recycle, don't duplicate ----------
     * The HTML renders each flash-sale product exactly once. JS moves the
     * track left at a steady speed, and whenever the first card drifts fully
     * off the left edge it is appended to the end of the track and the
     * translation is compensated by its own width + gap.
     */
    function initFlashMarquee() {
        const container = document.querySelector('.flash-scroll-container');
        const track = document.querySelector('.flash-products-track');
        if (!container || !track) return;

        const cards = Array.from(track.children);
        if (cards.length === 0) return;

        const GAP = 16;            // px, must match `.flash-products-track { gap: 16px }`
        const BASE_SPEED = 0.55;   // px per frame (~33 px/sec at 60fps)
        let pos = 0;
        let paused = false;
        let rafId = null;
        let lastTs = null;
        let warmed = false;

        function tick(ts) {
            if (lastTs == null) lastTs = ts;
            const dt = Math.min(ts - lastTs, 50);
            lastTs = ts;

            if (!paused && warmed) {
                const pxThisFrame = BASE_SPEED * (dt / 16.6667);
                pos -= pxThisFrame;

                let first = track.firstElementChild;
                while (first && first.offsetLeft + first.offsetWidth + pos < 0) {
                    pos += first.offsetWidth + GAP;
                    track.appendChild(first);
                    first = track.firstElementChild;
                }

                track.style.transform = 'translate3d(' + pos.toFixed(2) + 'px, 0, 0)';
            }

            rafId = requestAnimationFrame(tick);
        }

        container.addEventListener('mouseenter', function() { paused = true; });
        container.addEventListener('mouseleave', function() { paused = false; });
        document.addEventListener('visibilitychange', function() {
            lastTs = null;
        });

        function warmUp() {
            warmed = true;
            lastTs = null;
            rafId = requestAnimationFrame(tick);
        }

        if (document.readyState === 'complete') {
            warmUp();
        } else {
            window.addEventListener('load', warmUp, { once: true });
        }
    }

})();