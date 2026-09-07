<?php
//Tracks how many pages each book counts toward the user's "pages this week" goal.
//keeps the number on the users table in sync when books are added, edited, or deleted.

//calendar week id used everywhere (example: 2026W15).
function shelfie_week_key(): string
{
    return date('o') . 'W' . str_pad((string)date('W'), 2, '0', STR_PAD_LEFT);
}

//Current page capped by total pages (at least 1 page so math never breaks).
function shelfie_effective_pages(int $currentPage, int $totalPages): int
{
    $tp = max(1, $totalPages);
    return max(0, min($currentPage, $tp));
}

//writes the user's weekly page total from the sum of all their books this week.
function shelfie_sync_user_week_pages(mysqli $con, int $userId, string $weekKey): void
{
    $stmt = $con->prepare('SELECT COALESCE(SUM(pages), 0) AS s FROM book_weekly_pages WHERE user_id = ? AND week_key = ?');
    $stmt->bind_param('is', $userId, $weekKey);
    $stmt->execute();
    $sum = (int)$stmt->get_result()->fetch_assoc()['s'];
    $stmt->close();

    $u = $con->prepare('UPDATE users SET pages_read_week_total = ?, pages_week_key = ? WHERE id = ?');
    $u->bind_param('isi', $sum, $weekKey, $userId);
    $u->execute();
    $u->close();
}

// adds or removes pages for one book this week (can be negative when the user lowers their page count).
function shelfie_apply_weekly_book_delta(mysqli $con, int $userId, int $bookId, string $weekKey, int $signedDelta): void
{
    if ($signedDelta === 0) {
        return;
    }

    $stmt = $con->prepare('SELECT pages FROM book_weekly_pages WHERE book_id = ? AND week_key = ? LIMIT 1');
    $stmt->bind_param('is', $bookId, $weekKey);
    $stmt->execute();
    $res = $stmt->get_result();
    $row = $res->fetch_assoc();
    $stmt->close();

    $existing = $row !== null ? (int)$row['pages'] : null;
    $base = $existing ?? 0;
    $newPages = max(0, $base + $signedDelta);

    if ($existing !== null) {
        if ($newPages === 0) {
            $d = $con->prepare('DELETE FROM book_weekly_pages WHERE book_id = ? AND week_key = ?');
            $d->bind_param('is', $bookId, $weekKey);
            $d->execute();
            $d->close();
        } else {
            $up = $con->prepare('UPDATE book_weekly_pages SET pages = ? WHERE book_id = ? AND week_key = ?');
            $up->bind_param('iis', $newPages, $bookId, $weekKey);
            $up->execute();
            $up->close();
        }
    } elseif ($newPages > 0) {
        $ins = $con->prepare('INSERT INTO book_weekly_pages (user_id, book_id, week_key, pages) VALUES (?, ?, ?, ?)');
        $ins->bind_param('iisi', $userId, $bookId, $weekKey, $newPages);
        $ins->execute();
        $ins->close();
    }
}
