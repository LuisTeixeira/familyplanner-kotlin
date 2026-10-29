document.addEventListener('htmx:configRequest', function (evt) {
    const match = document.cookie.match(/(?:^|; )csrf_token=([^;]*)/);
    if (match) {
        evt.detail.headers['X-CSRF-Token'] = decodeURIComponent(match[1]);
    }
});