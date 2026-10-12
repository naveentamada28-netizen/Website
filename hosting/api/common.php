<?php
declare(strict_types=1);

header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: Content-Type, Authorization');
header('Access-Control-Allow-Methods: POST, OPTIONS');

if (($_SERVER['REQUEST_METHOD'] ?? '') === 'OPTIONS') {
    http_response_code(204);
    exit;
}

function data_dir(): string
{
    $dir = dirname(__DIR__) . '/data';
    if (!is_dir($dir) && !mkdir($dir, 0775, true) && !is_dir($dir)) {
        fail(500, 'Storage is not available.');
    }
    return $dir;
}

function read_json(string $file): array
{
    if (!is_file($file)) {
        return [];
    }
    $decoded = json_decode((string) file_get_contents($file), true);
    return is_array($decoded) ? $decoded : [];
}

function write_json(string $file, array $data): void
{
    $handle = fopen($file, 'c+');
    if ($handle === false) {
        fail(500, 'Could not store the account.');
    }
    flock($handle, LOCK_EX);
    ftruncate($handle, 0);
    rewind($handle);
    fwrite($handle, json_encode($data, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES));
    fflush($handle);
    flock($handle, LOCK_UN);
    fclose($handle);
}

function request_body(): array
{
    $decoded = json_decode((string) file_get_contents('php://input'), true);
    if (is_array($decoded)) {
        return $decoded;
    }
    return is_array($_POST) ? $_POST : [];
}

function digits(string $value): string
{
    return preg_replace('/\D+/', '', $value) ?? '';
}

function fail(int $status, string $message): void
{
    http_response_code($status);
    echo json_encode(['ok' => false, 'message' => $message]);
    exit;
}

function ok(array $payload): void
{
    echo json_encode(array_merge(['ok' => true], $payload));
    exit;
}
