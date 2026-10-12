<?php
declare(strict_types=1);
require __DIR__ . '/common.php';

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    fail(405, 'Use POST.');
}

$body = request_body();
$mobile = digits((string) ($body['mobile'] ?? ''));
$password = (string) ($body['password'] ?? '');

if (strlen($mobile) < 10 || strlen($mobile) > 15) {
    fail(422, 'Enter a mobile number with 10 to 15 digits.');
}
if (strlen($password) < 6) {
    fail(422, 'Use at least 6 characters for the password.');
}

$file = data_dir() . '/users.json';
$users = read_json($file);
foreach ($users as $user) {
    if (($user['mobile'] ?? '') === $mobile) {
        fail(409, 'That mobile number is already registered.');
    }
}

$users[] = [
    'mobile' => $mobile,
    'password_hash' => password_hash($password, PASSWORD_DEFAULT),
    'created_at' => gmdate('c'),
];
write_json($file, $users);
ok(['message' => 'Account created.']);
