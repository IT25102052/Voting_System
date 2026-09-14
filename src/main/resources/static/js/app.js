/* Reality Vote — shared front-end behaviors.
   Vanilla JS, no build step. Every enhancement here is progressive:
   if this file fails to load, every page still works exactly as
   it did with plain server-rendered HTML + Bootstrap. */
(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        initToasts();
        initVoteForms();
        initFormLoadingStates();
        initSortableTables();
        initPasswordToggles();
        initSpotlightCards();
        initPageTransitions();
        initScoreSliders();
    });

    /* ---------- Toasts ---------- */
    function toastContainer() {
        var c = document.getElementById('rv-toast-container');
        if (!c) {
            c = document.createElement('div');
            c.id = 'rv-toast-container';
            c.className = 'rv-toast-container';
            c.setAttribute('aria-live', 'polite');
            document.body.appendChild(c);
        }
        return c;
    }

    function showToast(message, type) {
        if (!message) return;
        var c = toastContainer();
        var t = document.createElement('div');
        t.className = 'rv-toast rv-toast-' + (type || 'success');
        var icon = type === 'danger' ? 'fa-circle-exclamation' : 'fa-circle-check';
        t.innerHTML = '<i class="fa-solid ' + icon + '"></i><span></span>';
        t.querySelector('span').textContent = message;
        c.appendChild(t);
        requestAnimationFrame(function () { t.classList.add('show'); });
        setTimeout(function () {
            t.classList.remove('show');
            setTimeout(function () { t.remove(); }, 300);
        }, 4500);
    }
    window.RV = window.RV || {};
    window.RV.toast = showToast;

    // Server-rendered flash alerts (.alert-success / .alert-danger) already
    // work with zero JS. We additionally surface them as a toast for a more
    // "live platform" feel, without removing the original accessible alert.
    function initToasts() {
        document.querySelectorAll('.alert-success, .alert-danger').forEach(function (el) {
            var text = el.textContent.trim();
            if (!text) return;
            var type = el.classList.contains('alert-danger') ? 'danger' : 'success';
            showToast(text, type);
        });
    }

    /* ---------- Voting interaction (#9) ---------- */
    // Backend voting rules are untouched — this only improves what happens
    // client-side around the real POST: locking the UI so a double-click
    // can't fire two submissions, and giving immediate visual feedback
    // while the (real) request is in flight.
    function initVoteForms() {
        document.querySelectorAll('form.js-vote-form').forEach(function (form) {
            form.addEventListener('submit', function () {
                var group = document.querySelectorAll('form.js-vote-form');
                var card = form.closest('.js-vote-card');
                if (card) card.classList.add('rv-selected');
                group.forEach(function (f) {
                    var btn = f.querySelector('button[type="submit"]');
                    if (!btn) return;
                    btn.disabled = true;
                    if (f === form) {
                        btn.dataset.originalHtml = btn.innerHTML;
                        btn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Casting vote…';
                    } else {
                        btn.classList.add('rv-btn-dimmed');
                    }
                });
            });
        });
    }

    /* ---------- Sortable tables (#15) ---------- */
    // Opt-in: add class="rv-sortable" to a <table>. Click a <th> to sort by
    // that column (client-side only — no backend change, no data alteration).
    function initSortableTables() {
        document.querySelectorAll('table.rv-sortable').forEach(function (table) {
            var tbody = table.querySelector('tbody');
            if (!tbody) return;
            table.querySelectorAll('thead th').forEach(function (th, index) {
                th.classList.add('rv-th-sortable');
                th.setAttribute('role', 'button');
                th.setAttribute('tabindex', '0');
                var activate = function () {
                    var rows = Array.prototype.slice.call(tbody.querySelectorAll('tr'));
                    var asc = th.dataset.sortDir !== 'asc';
                    table.querySelectorAll('thead th').forEach(function (h) {
                        h.dataset.sortDir = '';
                        h.classList.remove('rv-sort-asc', 'rv-sort-desc');
                    });
                    th.dataset.sortDir = asc ? 'asc' : 'desc';
                    th.classList.add(asc ? 'rv-sort-asc' : 'rv-sort-desc');
                    rows.sort(function (a, b) {
                        var av = (a.children[index] && a.children[index].textContent.trim()) || '';
                        var bv = (b.children[index] && b.children[index].textContent.trim()) || '';
                        var an = parseFloat(av.replace(/[^0-9.\-]/g, ''));
                        var bn = parseFloat(bv.replace(/[^0-9.\-]/g, ''));
                        var cmp = (!isNaN(an) && !isNaN(bn)) ? (an - bn) : av.localeCompare(bv);
                        return asc ? cmp : -cmp;
                    });
                    rows.forEach(function (r) { tbody.appendChild(r); });
                };
                th.addEventListener('click', activate);
                th.addEventListener('keydown', function (e) {
                    if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); activate(); }
                });
            });
        });
    }

    /* ---------- Password visibility toggle ---------- */
    function initPasswordToggles() {
        document.querySelectorAll('input[type="password"]').forEach(function (input) {
            if (input.dataset.toggleAttached) return;
            input.dataset.toggleAttached = 'true';
            var wrap = document.createElement('div');
            wrap.className = 'rv-password-wrap';
            input.parentNode.insertBefore(wrap, input);
            wrap.appendChild(input);
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'rv-password-toggle';
            btn.setAttribute('aria-label', 'Show password');
            btn.innerHTML = '<i class="fa-solid fa-eye"></i>';
            wrap.appendChild(btn);
            btn.addEventListener('click', function () {
                var showing = input.type === 'text';
                input.type = showing ? 'password' : 'text';
                btn.innerHTML = showing ? '<i class="fa-solid fa-eye"></i>' : '<i class="fa-solid fa-eye-slash"></i>';
                btn.setAttribute('aria-label', showing ? 'Show password' : 'Hide password');
            });
        });
    }

    /* ---------- Cursor-spotlight hover + parallax tilt (#6 / #34) ---------- */
    function initSpotlightCards() {
        if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
        if (window.matchMedia('(hover: none)').matches) return; // skip on touch devices
        var maxTilt = 6; // degrees — subtle, not gimmicky
        document.querySelectorAll('.show-card, .podium-step, .kpi-card').forEach(function (card) {
            card.classList.add('rv-spotlight', 'rv-tilt');
            card.addEventListener('mousemove', function (e) {
                var r = card.getBoundingClientRect();
                var x = e.clientX - r.left, y = e.clientY - r.top;
                card.style.setProperty('--mx', x + 'px');
                card.style.setProperty('--my', y + 'px');
                var px = (x / r.width) - 0.5;  // -0.5 .. 0.5
                var py = (y / r.height) - 0.5;
                var rotY = px * maxTilt * 2;
                var rotX = -py * maxTilt * 2;
                card.classList.add('rv-tilting');
                card.style.transform = 'perspective(900px) rotateX(' + rotX.toFixed(2) + 'deg) rotateY(' + rotY.toFixed(2) + 'deg) translateY(-2px)';
            });
            card.addEventListener('mouseleave', function () {
                card.classList.remove('rv-tilting');
                card.style.transform = '';
            });
        });
    }

    /* ---------- Lightweight page-fade transitions (#35) ---------- */
    function initPageTransitions() {
        if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;
        document.documentElement.classList.add('rv-page-ready');
        document.addEventListener('click', function (e) {
            var a = e.target.closest('a[href]');
            if (!a) return;
            if (a.target && a.target !== '' && a.target !== '_self') return;
            if (a.hasAttribute('download')) return;
            if (e.metaKey || e.ctrlKey || e.shiftKey || e.altKey || e.button !== 0) return;
            var href = a.getAttribute('href');
            if (!href || href.startsWith('#') || href.startsWith('javascript:') || href.startsWith('mailto:') || href.startsWith('tel:')) return;
            var url;
            try { url = new URL(href, window.location.href); } catch (err) { return; }
            if (url.origin !== window.location.origin) return;
            if (url.pathname === window.location.pathname && url.search === window.location.search) return;
            e.preventDefault();
            document.documentElement.classList.add('rv-page-leaving');
            setTimeout(function () { window.location.href = href; }, 140);
        });
    }
    /* ---------- Generic button loading state (#18/#24) ---------- */
    // Any non-GET form's submit button gets a spinner on submit — a plain
    // server-rendered-page pattern, no fetch/AJAX involved. Vote forms are
    // handled separately (initVoteForms) so they aren't double-processed.
    function initFormLoadingStates() {
        document.querySelectorAll('form:not(.js-vote-form)').forEach(function (form) {
            var method = (form.getAttribute('method') || 'get').toLowerCase();
            if (method !== 'post') return;
            form.addEventListener('submit', function () {
                if (form.dataset.rvSubmitted === 'true') return; // avoid double state on repeat submit
                var btn = form.querySelector('button[type="submit"], button:not([type])');
                if (!btn) return;
                form.dataset.rvSubmitted = 'true';
                btn.dataset.originalHtml = btn.innerHTML;
                btn.disabled = true;
                btn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> ' + (btn.textContent.trim() || 'Please wait…');
            });
        });
    }

    /* ---------- Judge score slider sync (#16) ---------- */
    // The <input type="number" name="scoreValue"> is still the field the
    // backend reads — the slider is a visual layer synced on top of it,
    // so scoring still works exactly the same with JS disabled.
    function initScoreSliders() {
        document.querySelectorAll('.js-score-slider').forEach(function (slider) {
            var wrap = slider.closest('form') || slider.parentElement.parentElement;
            var readout = wrap.querySelector('.js-score-readout');
            var number = wrap.querySelector('.js-score-number');
            if (number && number.value !== '') slider.value = number.value;
            function paint(v) {
                var pct = Math.max(0, Math.min(100, Number(v) || 0));
                var color = pct >= 70 ? '#00e5ff' : pct >= 40 ? '#ffc857' : '#ff5c72';
                slider.style.background = 'linear-gradient(90deg, ' + color + ' 0%, ' + color + ' ' + pct + '%, rgba(255,255,255,.12) ' + pct + '%, rgba(255,255,255,.12) 100%)';
                if (readout) readout.textContent = pct;
                if (readout) readout.style.color = color;
            }
            paint(slider.value);
            slider.addEventListener('input', function () {
                if (number) number.value = slider.value;
                paint(slider.value);
            });
            if (number) {
                number.addEventListener('input', function () {
                    if (number.value === '') return;
                    slider.value = number.value;
                    paint(slider.value);
                });
            }
        });
    }
})();
