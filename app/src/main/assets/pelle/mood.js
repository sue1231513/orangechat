// Pelle d'Umore — Emotional Skin for AI Chat | CC BY 4.0 — Attribution required
// Trimmed for OrangeChat WebView mode: just the set/get core, no decode/glitch

(function () {
  'use strict';
  var _mode = 'off';

  window.Pelle = {
    set: function (mode) {
      mode = mode || 'off';
      document.body.setAttribute('data-mood', mode);
      _mode = mode;
    },
    get: function () { return _mode; }
  };
})();