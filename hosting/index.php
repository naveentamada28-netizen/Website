<?php declare(strict_types=1); ?>
<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>NT™ Geo Camera</title>
<style>
:root { --bg:#f4f7fb; --ink:#1b2430; --muted:#5c6b7a; --line:#d5e0ee; --card:#fff; --blue:#1f6feb; --gold:#a8842d; }
* { box-sizing:border-box; }
body { margin:0; min-height:100vh; font:16px/1.5 "Segoe UI", sans-serif; color:var(--ink); background:
  radial-gradient(circle at 10% -10%, #d7e6ff 0, transparent 36%),
  linear-gradient(180deg, #f8fbff, var(--bg)); }
main { width:min(440px, 92%); margin:48px auto; background:var(--card); border:1px solid var(--line); border-radius:28px; padding:28px 24px 22px; box-shadow:0 18px 40px rgba(31,111,235,.08); }
img { width:112px; height:112px; display:block; margin:0 auto 8px; }
h1 { text-align:center; margin:0; font-size:1.7rem; letter-spacing:-.03em; }
h1 span { color:var(--gold); font-size:.72rem; vertical-align:super; }
p { text-align:center; color:var(--muted); margin:6px 0 18px; }
label { display:block; font-size:.82rem; font-weight:700; color:var(--muted); margin:12px 0 6px; }
input { width:100%; border:1px solid var(--line); border-radius:12px; padding:12px 14px; font:inherit; color:var(--ink); background:#fbfcfe; }
input:focus { outline:3px solid rgba(31,111,235,.18); border-color:var(--blue); }
.row { display:flex; gap:10px; margin-top:16px; }
button { flex:1; border:0; border-radius:12px; padding:12px; font:inherit; font-weight:800; cursor:pointer; }
.primary { background:var(--blue); color:#fff; }
.ghost { background:#e7eef8; color:var(--ink); }
#status { min-height:1.4em; text-align:center; margin-top:14px; color:var(--muted); }
</style>
</head>
<body>
<main>
  <img src="nt-mark.png" alt="NT trademark">
  <h1>NT<span>TM</span> Geo Camera</h1>
  <p>Mobile number and password are stored on this hosting website. The password is saved as a hash, not as readable text.</p>
  <label for="mobile">Mobile number</label>
  <input id="mobile" inputmode="tel" autocomplete="username" placeholder="10 to 15 digits">
  <label for="password">Password</label>
  <input id="password" type="password" autocomplete="current-password" placeholder="At least 6 characters">
  <div class="row">
    <button class="ghost" id="register" type="button">Create account</button>
    <button class="primary" id="login" type="button">Login</button>
  </div>
  <p id="status" role="status"></p>
</main>
<script>
const status = document.getElementById('status');
async function send(path) {
  status.textContent = 'Contacting the website…';
  const body = {
    mobile: document.getElementById('mobile').value,
    password: document.getElementById('password').value
  };
  try {
    const response = await fetch(path, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
    const data = await response.json();
    status.textContent = data.message || (data.ok ? 'Signed in.' : 'Could not sign in.');
  } catch (error) {
    status.textContent = 'Could not reach this website.';
  }
}
document.getElementById('login').onclick = () => send('api/login.php');
document.getElementById('register').onclick = () => send('api/register.php');
</script>
</body>
</html>
