<?php
declare(strict_types=1);
require __DIR__ . '/common.php';

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    fail(405, 'Use POST.');
}

$body = request_body();
$mobile = digits((string) ($body['mobile'] ?? ''));
$password = (string) ($body['password'] ?? '');
$users = read_json(data_dir() . '/users.json');
$match = null;
foreach ($users as $user) {
    if (($user['mobile'] ?? '') === $mobile) {
        $match = $user;
        break;
    }
}

if ($match === null || !password_verify($password, (string) ($match['password_hash'] ?? ''))) {
    fail(401, 'Check the mobile number and password.');
}

$token = bin2hex(random_bytes(32));
$sessionsFile = data_dir() . '/sessions.json';
$sessions = read_json($sessionsFile);
$sessions = array_values(array_filter($sessions, static function ($session) use ($mobile) {
    return ($session['mobile'] ?? '') !== $mobile;
}));
$sessions[] = [
    'token' => $token,
    'mobile' => $mobile,
    'expires_at' => gmdate('c', time() + 60 * 60 * 24 * 30),
];
write_json($sessionsFile, $sessions);
ok(['token' => $token, 'mobile' => $mobile, 'message' => 'Signed in.']);
