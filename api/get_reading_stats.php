<?php
header('Content-Type: application/json');
require_once __DIR__ . '/connection.php';
require_once __DIR__ . '/weekly_pages.inc.php';

$userId = intval($_GET['user_id'] ?? 0);
if ($userId <= 0) {
    echo json_encode(['success' => false, 'message' => 'Invalid user']);
    exit;
}

$out = ['success' => true];

// Load goal settings and stored weekly page total for the home goal banner
$stmt = $con->prepare('
    SELECT reading_goal_type, reading_goal_value, pages_read_week_total, pages_week_key
    FROM users WHERE id = ? LIMIT 1
');
$stmt->bind_param('i', $userId);
$stmt->execute();
$urow = $stmt->get_result()->fetch_assoc();
$stmt->close();

$goalType = $urow['reading_goal_type'] ?? null;
$goalValue = isset($urow['reading_goal_value']) ? (int)$urow['reading_goal_value'] : 0;
$weekTotal = (int)($urow['pages_read_week_total'] ?? 0);
$weekKey = $urow['pages_week_key'] ?? '';

// Current ISO week label; must match add/update/delete book scripts
$currentWeekKey = shelfie_week_key();

$wkStmt = $con->prepare(
    'SELECT COUNT(*) AS c, COALESCE(SUM(CASE WHEN week_key = ? THEN pages ELSE 0 END), 0) AS s
     FROM book_weekly_pages WHERE user_id = ?'
);
$wkStmt->bind_param('si', $currentWeekKey, $userId);
$wkStmt->execute();
$wkRow = $wkStmt->get_result()->fetch_assoc();
$wkStmt->close();
$weeklyRowCount = (int)($wkRow['c'] ?? 0);
$weeklySumFromTable = (int)($wkRow['s'] ?? 0);

// Prefer per-book weekly rows when present; otherwise fall back to legacy users.pages_read_week_total
$pagesThisWeek = $weeklyRowCount > 0
    ? $weeklySumFromTable
    : (($weekKey === $currentWeekKey) ? $weekTotal : 0);

$out['reading_goal'] = [
    'type' => $goalType,
    'value' => $goalValue,
    'pages_read_this_week' => $pagesThisWeek,
    'week_key' => $currentWeekKey,
];

$yearStart = date('Y') . '-01-01';
// Count finished books that count toward this year’s goal (legacy rows without completed_at still count)
$stmt = $con->prepare("
    SELECT COUNT(*) AS c FROM books
    WHERE user_id = ?
      AND LOWER(TRIM(status)) IN ('completed', 'finished')
      AND (completed_at IS NULL OR completed_at >= ?)
");
$stmt->bind_param('is', $userId, $yearStart);
$stmt->execute();
$booksThisYear = (int)$stmt->get_result()->fetch_assoc()['c'];
$stmt->close();

$out['reading_goal']['books_finished_this_year'] = $booksThisYear;

// Sum min(current_page, total_pages) across shelf so progress never exceeds each book’s length
$stmt = $con->prepare('
    SELECT COALESCE(SUM(LEAST(current_page, GREATEST(total_pages, 1))), 0) AS pages
    FROM books WHERE user_id = ?
');
$stmt->bind_param('i', $userId);
$stmt->execute();
$pagesSum = (int)$stmt->get_result()->fetch_assoc()['pages'];
$stmt->close();
$out['total_pages_progress'] = $pagesSum;

$finishedPerMonth = [];
// Group finished books by completion month for the stats chart (only rows with a real completed_at)
$stmt = $con->prepare("
    SELECT DATE_FORMAT(completed_at, '%Y-%m') AS ym, COUNT(*) AS c
    FROM books
    WHERE user_id = ?
      AND LOWER(TRIM(status)) IN ('completed', 'finished')
      AND completed_at IS NOT NULL
    GROUP BY ym
    ORDER BY ym DESC
    LIMIT 24
");
$stmt->bind_param('i', $userId);
$stmt->execute();
$res = $stmt->get_result();
while ($row = $res->fetch_assoc()) {
    $finishedPerMonth[] = ['month' => $row['ym'], 'count' => (int)$row['c']];
}
$stmt->close();
$out['books_finished_per_month'] = $finishedPerMonth;

// How many finished books lack completed_at so the app can show a migration hint
$stmt = $con->prepare("
    SELECT COUNT(*) AS c FROM books
    WHERE user_id = ?
      AND LOWER(TRIM(status)) IN ('completed', 'finished')
      AND completed_at IS NULL
");
$stmt->bind_param('i', $userId);
$stmt->execute();
$out['finished_books_without_completed_at'] = (int)$stmt->get_result()->fetch_assoc()['c'];
$stmt->close();

// Pull genre strings and categories to build frequency counts in PHP
$genreCounts = [];
$stmt = $con->prepare('SELECT genres, category FROM books WHERE user_id = ?');
$stmt->bind_param('i', $userId);
$stmt->execute();
$res = $stmt->get_result();
while ($row = $res->fetch_assoc()) {
    $g = trim((string)($row['genres'] ?? ''));
    if ($g !== '') {
        foreach (preg_split('/\s*,\s*/', $g) as $part) {
            $part = trim($part);
            if ($part === '') {
                continue;
            }
            $key = $part;
            $genreCounts[$key] = ($genreCounts[$key] ?? 0) + 1;
        }
    } else {
        $c = trim((string)($row['category'] ?? ''));
        if ($c !== '') {
            $genreCounts[$c] = ($genreCounts[$c] ?? 0) + 1;
        }
    }
}
$stmt->close();
arsort($genreCounts);
$out['genre_breakdown'] = $genreCounts;

// All days the user opened the app (newest first) for streak math and 90-day heatmap
$stmt = $con->prepare('SELECT log_date FROM reading_logs WHERE user_id = ? ORDER BY log_date DESC');
$stmt->bind_param('i', $userId);
$stmt->execute();
$res = $stmt->get_result();
$dates = [];
while ($row = $res->fetch_assoc()) {
    $dates[] = $row['log_date'];
}
$stmt->close();

// Walk consecutive calendar days backward from today while each day exists in reading_logs
$streak = 0;
$current = strtotime(date('Y-m-d'));
for ($i = 0; $i < count($dates); $i++) {
    $dbDate = strtotime($dates[$i]);
    if (date('Y-m-d', $dbDate) === date('Y-m-d', $current)) {
        $streak++;
        $current = strtotime('-1 day', $current);
    } else {
        break;
    }
}
$out['current_streak'] = $streak;

// Build 90 booleans for whether each past day had a reading log entry
$loggedSet = array_flip($dates);
$history = [];
for ($d = 89; $d >= 0; $d--) {
    $day = date('Y-m-d', strtotime("-$d days"));
    $history[] = [
        'date' => $day,
        'read' => isset($loggedSet[$day]) ? 1 : 0,
    ];
}
$out['streak_history_90d'] = $history;

echo json_encode($out);
