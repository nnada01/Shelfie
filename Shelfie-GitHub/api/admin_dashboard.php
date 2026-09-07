<?php
//Shelfie admin web dashboard 

declare(strict_types=1);

session_start();

if (!is_readable(__DIR__ . '/connection.php')) {
    http_response_code(500);
    header('Content-Type: text/html; charset=UTF-8');
    echo '<!DOCTYPE html><html><head><meta charset="utf-8"><title>Shelfie Admin</title></head><body>';
    echo '<p>Create <code>connection.php</code> in this folder.</p></body></html>';
    exit;
}

require_once __DIR__ . '/connection.php';

if (!isset($con) || !$con instanceof mysqli) {
    http_response_code(500);
    header('Content-Type: text/html; charset=UTF-8');
    echo '<!DOCTYPE html><html><body><p>Database connection failed.</p></body></html>';
    exit;
}

$mysqli = $con;

function h(string $s): string
{
    return htmlspecialchars($s, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
}

function user_search_blob(array $u): string
{
    $parts = [
        (string)($u['email'] ?? ''),
        (string)($u['first_name'] ?? ''),
        (string)($u['last_name'] ?? ''),
        (string)($u['id'] ?? ''),
        (string)($u['country'] ?? ''),
    ];
    return strtolower(implode(' ', $parts));
}

if (isset($_POST['logout'])) {
    $_SESSION = [];
    if (ini_get('session.use_cookies')) {
        $p = session_get_cookie_params();
        setcookie(session_name(), '', time() - 42000, $p['path'], $p['domain'], $p['secure'], $p['httponly']);
    }
    session_destroy();
    header('Location: admin_dashboard.php');
    exit;
}

$loginError = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['email'], $_POST['password'])) {
    $email = trim((string)$_POST['email']);
    $password = (string)$_POST['password'];
    if ($email === '' || $password === '') {
        $loginError = 'Enter email and password.';
    } else {
        $stmt = $mysqli->prepare('SELECT id, password, is_admin FROM users WHERE email = ? LIMIT 1');
        if ($stmt) {
            $stmt->bind_param('s', $email);
            $stmt->execute();
            $res = $stmt->get_result();
            $row = $res ? $res->fetch_assoc() : null;
            $stmt->close();
            if ($row && password_verify($password, (string)$row['password']) && (int)$row['is_admin'] === 1) {
                session_regenerate_id(true);
                $_SESSION['shelfie_admin_id'] = (int)$row['id'];
                header('Location: admin_dashboard.php');
                exit;
            }
        }
        $loginError = 'Invalid credentials or not an admin account.';
    }
}

$isAuthed = !empty($_SESSION['shelfie_admin_id']);

// ----- Login page -----
if (!$isAuthed) {
    header('Content-Type: text/html; charset=UTF-8');
    ?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Shelfie — Admin sign in</title>
    <style>
        /* Matches Android res/values/colors.xml (light) and values-night/colors.xml (dark) */
        :root {
            --primary: #2F4736;
            --on-primary: #FFFFFF;
            --secondary: #6E4A32;
            --tertiary: #C18E3A;
            --bg: #FCF5EB;
            --text: #1F1A14;
            --muted: #53473C;
            --surface: #FCF5EB;
            --surface-variant: #E2D8CB;
            --card: #EEDCC3;
            --border: #8A7B6C;
            --border-soft: #D5C8B7;
            --error: #BA1A1A;
            --glow: rgba(47, 71, 54, 0.18);
        }
        [data-theme="dark"] {
            --primary: #8BBFA4;
            --on-primary: #FFFFFF;
            --secondary: #C9B199;
            --tertiary: #D9BD73;
            --bg: #0F0F0D;
            --text: #FFFFFF;
            --muted: #B8B8B0;
            --surface: #151613;
            --surface-variant: #2F332D;
            --card: #2C3D34;
            --border: #8A8275;
            --border-soft: #3A3E35;
            --error: #FFB4AB;
            --glow: rgba(139, 191, 164, 0.22);
        }
        * { box-sizing: border-box; }
        body {
            margin: 0; min-height: 100vh;
            font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            background: var(--bg);
            color: var(--text);
            display: flex; align-items: center; justify-content: center; padding: 1.5rem;
        }
        .panel {
            width: 100%; max-width: 420px;
            background: var(--surface);
            border: 1px solid var(--border-soft);
            border-radius: 20px;
            padding: 2rem 2rem 1.75rem;
            box-shadow: 0 12px 40px rgba(47, 71, 54, 0.08);
        }
        [data-theme="dark"] .panel { box-shadow: 0 12px 40px rgba(0,0,0,0.5); border-color: var(--border); }
        .brand {
            font-size: 1.65rem;
            font-weight: 700;
            color: var(--primary);
            letter-spacing: -0.02em;
            margin-bottom: 0.25rem;
        }
        .brand span { color: var(--tertiary); font-weight: 600; }
        h1 { margin: 0 0 0.35rem; font-size: 1.1rem; font-weight: 600; color: var(--text); }
        .sub { margin: 0 0 1.5rem; font-size: 0.9rem; color: var(--muted); line-height: 1.5; }
        label { display: block; font-size: 0.8rem; font-weight: 600; margin-bottom: 0.4rem; color: var(--muted); text-transform: uppercase; letter-spacing: 0.04em; }
        input[type="email"], input[type="password"] {
            width: 100%; padding: 0.75rem 0.9rem; margin-bottom: 1rem; border-radius: 12px;
            border: 1px solid var(--border-soft); background: var(--bg); color: var(--text); font-size: 1rem;
            transition: border-color 0.15s, box-shadow 0.15s;
        }
        [data-theme="dark"] input[type="email"],
        [data-theme="dark"] input[type="password"] { border-color: var(--border); background: var(--surface); }
        input:focus { outline: none; border-color: var(--primary); box-shadow: 0 0 0 3px var(--glow); }
        button[type="submit"] {
            width: 100%; padding: 0.85rem; border: none; border-radius: 12px;
            background: var(--primary); color: var(--on-primary); font-weight: 700; font-size: 1rem;
            cursor: pointer; font-family: inherit;
            transition: transform 0.12s, filter 0.12s;
        }
        button[type="submit"]:hover { filter: brightness(1.08); transform: translateY(-1px); }
        .err {
            color: var(--error);
            font-size: 0.88rem;
            margin-bottom: 1rem;
            padding: 0.65rem 0.75rem;
            background: rgba(186, 26, 26, 0.1);
            border-radius: 10px;
        }
        [data-theme="dark"] .err { background: rgba(255, 180, 171, 0.12); color: var(--error); }
        .theme-toggle-login {
            position: fixed; top: 1rem; right: 1rem;
            background: var(--surface); border: 1px solid var(--border-soft); border-radius: 999px;
            padding: 0.5rem 0.85rem; font-size: 0.85rem; cursor: pointer; font-family: inherit; color: var(--text);
        }
        [data-theme="dark"] .theme-toggle-login { border-color: var(--border); background: var(--surface-variant); }
    </style>
</head>
<body>
    <button type="button" class="theme-toggle-login" id="themeLogin" aria-label="Toggle theme">◐ Theme</button>
    <div class="panel">
        <div class="brand">Shelfie <span>admin</span></div>
        <h1>Sign in</h1>
        <p class="sub">Use an account with admin privileges (<code>is_admin = 1</code> in the database).</p>
        <?php if ($loginError !== ''): ?><div class="err"><?= h($loginError) ?></div><?php endif; ?>
        <form method="post" action="admin_dashboard.php" autocomplete="on">
            <label for="email">Email</label>
            <input id="email" name="email" type="email" required autocomplete="username">
            <label for="password">Password</label>
            <input id="password" name="password" type="password" required autocomplete="current-password">
            <button type="submit">Enter dashboard</button>
        </form>
    </div>
    <script>
    (function(){
        var k = 'shelfie_admin_theme';
        var t = localStorage.getItem(k);
        if (t === 'dark') document.documentElement.setAttribute('data-theme','dark');
        document.getElementById('themeLogin').onclick = function(){
            var d = document.documentElement.getAttribute('data-theme') === 'dark';
            if (d) { document.documentElement.removeAttribute('data-theme'); localStorage.setItem(k,'light'); }
            else { document.documentElement.setAttribute('data-theme','dark'); localStorage.setItem(k,'dark'); }
        };
    })();
    </script>
</body>
</html>
    <?php
    exit;
}

// ----- Optional JSON export -----
if (isset($_GET['export']) && $_GET['export'] === 'json') {
    $hasJournals = false;
    if ($chk = $mysqli->query("SHOW TABLES LIKE 'journals'")) {
        $hasJournals = $chk->num_rows > 0;
        $chk->close();
    }
    $journalSub = $hasJournals ? '(SELECT COUNT(*) FROM journals j WHERE j.user_id = u.id)' : '0';
    $sqlUsers = "
        SELECT u.id, u.email, u.first_name, u.last_name, u.birthday, u.country,
               u.reading_goal_type, u.reading_goal_value, u.is_admin,
               (SELECT COUNT(*) FROM books b WHERE b.user_id = u.id) AS book_count,
               $journalSub AS journal_count
        FROM users u ORDER BY u.id";
    $users = [];
    if ($uq = $mysqli->query($sqlUsers)) {
        while ($u = $uq->fetch_assoc()) {
            $users[] = $u;
        }
        $uq->close();
    }
    $books = [];
    if ($bq = $mysqli->query('SELECT id, user_id, title, author, category, status, total_pages, current_page,
           is_favorite, rating, genres, is_recommended FROM books ORDER BY user_id, id')) {
        while ($b = $bq->fetch_assoc()) {
            $books[] = $b;
        }
        $bq->close();
    }
    header('Content-Type: application/json; charset=UTF-8');
    header('Content-Disposition: attachment; filename="shelfie_admin_export_' . date('Y-m-d_His') . '.json"');
    echo json_encode([
        'exported_at' => date('c'),
        'users' => $users,
        'books' => $books,
    ], JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE);
    exit;
}

// ----- Data -----
$hasJournals = false;
if ($chk = $mysqli->query("SHOW TABLES LIKE 'journals'")) {
    $hasJournals = $chk->num_rows > 0;
    $chk->close();
}

$journalSub = $hasJournals ? '(SELECT COUNT(*) FROM journals j WHERE j.user_id = u.id)' : '0';

$sqlUsers = "
    SELECT
        u.id,
        u.email,
        u.first_name,
        u.last_name,
        u.birthday,
        u.country,
        u.reading_goal_type,
        u.reading_goal_value,
        u.is_admin,
        (SELECT COUNT(*) FROM books b WHERE b.user_id = u.id) AS book_count,
        $journalSub AS journal_count
    FROM users u
    ORDER BY u.id
";

$users = [];
if ($uq = $mysqli->query($sqlUsers)) {
    while ($u = $uq->fetch_assoc()) {
        $users[(int)$u['id']] = $u;
    }
    $uq->close();
}

$booksByUser = [];
$statusTotals = [];
$totalBookRows = 0;
$bq = $mysqli->query('
    SELECT id, user_id, title, author, category, status, total_pages, current_page,
           is_favorite, rating, genres, is_recommended
    FROM books
    ORDER BY user_id ASC, id ASC
');
if ($bq) {
    while ($b = $bq->fetch_assoc()) {
        $uid = (int)$b['user_id'];
        if (!isset($booksByUser[$uid])) {
            $booksByUser[$uid] = [];
        }
        $booksByUser[$uid][] = $b;
        $totalBookRows++;
        $st = trim((string)($b['status'] ?? 'Unknown'));
        if ($st === '') {
            $st = 'Unknown';
        }
        $statusTotals[$st] = ($statusTotals[$st] ?? 0) + 1;
    }
    $bq->close();
}

$adminSelf = ['email' => '', 'first_name' => ''];
$aid = (int)$_SESSION['shelfie_admin_id'];
if ($st = $mysqli->prepare('SELECT email, first_name FROM users WHERE id = ? LIMIT 1')) {
    $st->bind_param('i', $aid);
    $st->execute();
    $r = $st->get_result();
    if ($r && $row = $r->fetch_assoc()) {
        $adminSelf['email'] = (string)($row['email'] ?? '');
        $adminSelf['first_name'] = (string)($row['first_name'] ?? '');
    }
    $st->close();
}

$nUsers = count($users);
$nAdmins = 0;
$sumBooks = 0;
$sumJournals = 0;
foreach ($users as $u) {
    if ((int)($u['is_admin'] ?? 0) === 1) {
        $nAdmins++;
    }
    $sumBooks += (int)($u['book_count'] ?? 0);
    $sumJournals += (int)($u['journal_count'] ?? 0);
}

header('Content-Type: text/html; charset=UTF-8');
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Shelfie — Admin dashboard</title>
    <style>
        /* Light: res/values/colors.xml — Dark: res/values-night/colors.xml */
        :root {
            --primary: #2F4736;
            --on-primary: #FFFFFF;
            --secondary: #6E4A32;
            --tertiary: #C18E3A;
            --on-tertiary: #1F1300;
            --bg: #FCF5EB;
            --surface: #FCF5EB;
            --text: #1F1A14;
            --muted: #53473C;
            --card: #EEDCC3;
            --card-edge: #E2D8CB;
            --border: #8A7B6C;
            --border-soft: #D5C8B7;
            --brass: #C18E3A;
            --header-fg: #FCF5EB;
            --header-fg-muted: rgba(252, 245, 235, 0.88);
            --success: #2F4736;
            --reading: #4A6FA5;
            --want: #6E4A32;
            --header-shadow: rgba(47, 71, 54, 0.25);
            --table-head-bg: rgba(47, 71, 54, 0.1);
            --table-row-hover: rgba(47, 71, 54, 0.05);
        }
        [data-theme="dark"] {
            --primary: #8BBFA4;
            --on-primary: #FFFFFF;
            --secondary: #C9B199;
            --tertiary: #D9BD73;
            --on-tertiary: #332400;
            --bg: #0F0F0D;
            --surface: #151613;
            --text: #FFFFFF;
            --muted: #B8B8B0;
            --card: #2C3D34;
            --card-edge: #2F332D;
            --border: #8A8275;
            --border-soft: #3A3E35;
            --brass: #D9BD73;
            --header-fg: #000000;
            --header-fg-muted: rgba(0, 0, 0, 0.68);
            --success: #8BBFA4;
            --reading: #9EBBD4;
            --want: #C9B199;
            --header-shadow: rgba(0, 0, 0, 0.45);
            --table-head-bg: #2E4036;
            --table-head-fg: #CFE8D7;
            --table-row-hover: rgba(139, 191, 164, 0.08);
        }
        * { box-sizing: border-box; }
        body {
            margin: 0;
            font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            background: var(--bg);
            color: var(--text);
            line-height: 1.5;
            min-height: 100vh;
            display: flex;
            flex-direction: column;
        }
        .top-header {
            background: var(--primary);
            color: var(--header-fg);
            padding: 1rem 1.5rem;
            box-shadow: 0 4px 20px var(--header-shadow);
            position: sticky;
            top: 0;
            z-index: 40;
        }
        .top-header-inner {
            max-width: 1180px;
            margin: 0 auto;
            display: flex;
            flex-wrap: wrap;
            align-items: center;
            justify-content: space-between;
            gap: 1rem;
        }
        .top-header h1 {
            margin: 0;
            font-size: 1.25rem;
            font-weight: 700;
            letter-spacing: -0.02em;
            color: var(--header-fg);
        }
        .top-header .tagline { font-size: 0.82rem; color: var(--header-fg-muted); margin-top: 0.2rem; }
        .header-actions { display: flex; flex-wrap: wrap; align-items: center; gap: 0.5rem; }
        .btn {
            display: inline-flex;
            align-items: center;
            gap: 0.35rem;
            padding: 0.45rem 0.9rem;
            border-radius: 10px;
            font-size: 0.85rem;
            font-weight: 600;
            font-family: inherit;
            cursor: pointer;
            border: 1px solid transparent;
            text-decoration: none;
            transition: background 0.15s, transform 0.12s;
        }
        .btn-ghost {
            background: rgba(252, 245, 235, 0.12);
            color: var(--header-fg);
            border-color: rgba(252, 245, 235, 0.4);
        }
        .btn-ghost:hover { background: rgba(252, 245, 235, 0.22); }
        [data-theme="dark"] .btn-ghost {
            background: rgba(0, 0, 0, 0.08);
            color: var(--header-fg);
            border-color: rgba(0, 0, 0, 0.35);
        }
        [data-theme="dark"] .btn-ghost:hover { background: rgba(0, 0, 0, 0.14); }
        .btn-primary-lite {
            background: #E8DCC4;
            color: #1F1810;
            border: 1px solid rgba(47, 71, 54, 0.15);
        }
        .btn-primary-lite:hover { filter: brightness(1.03); transform: translateY(-1px); }
        [data-theme="dark"] .btn-primary-lite {
            background: #CFE8D7;
            color: #0F261B;
            border-color: rgba(15, 38, 27, 0.2);
        }
        .wrap { max-width: 1180px; margin: 0 auto; padding: 1.25rem 1.5rem 3rem; width: 100%; flex: 1; }
        .toolbar {
            display: flex;
            flex-wrap: wrap;
            gap: 0.75rem;
            align-items: center;
            margin-bottom: 1.25rem;
        }
        .search-wrap { flex: 1; min-width: 200px; max-width: 420px; position: relative; }
        .search-wrap input {
            width: 100%; padding: 0.65rem 1rem 0.65rem 2.5rem;
            border-radius: 12px;
            border: 1px solid var(--border-soft);
            background: var(--surface);
            color: var(--text);
            font-size: 0.95rem;
            font-family: inherit;
        }
        [data-theme="dark"] .search-wrap input { border-color: var(--border); background: var(--surface); }
        .search-wrap::before {
            content: "⌕";
            position: absolute;
            left: 0.85rem;
            top: 50%;
            transform: translateY(-50%);
            opacity: 0.45;
            font-size: 1.1rem;
            pointer-events: none;
        }
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
            gap: 1rem;
            margin-bottom: 1.5rem;
        }
        .stat-card {
            background: var(--surface);
            border: 1px solid var(--border-soft);
            border-radius: 16px;
            padding: 1.1rem 1.15rem;
            box-shadow: 0 2px 12px rgba(47, 71, 54, 0.06);
            transition: transform 0.15s, box-shadow 0.15s;
        }
        .stat-card:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(47, 71, 54, 0.1); }
        [data-theme="dark"] .stat-card { border-color: var(--border); box-shadow: 0 2px 16px rgba(0,0,0,0.4); }
        .stat-card .label { font-size: 0.72rem; text-transform: uppercase; letter-spacing: 0.06em; color: var(--muted); font-weight: 600; }
        .stat-card .value { font-size: 1.65rem; font-weight: 700; color: var(--primary); font-variant-numeric: tabular-nums; margin-top: 0.25rem; }
        .stat-card .hint { font-size: 0.78rem; color: var(--muted); margin-top: 0.35rem; }
        .status-strip {
            background: var(--surface);
            border: 1px solid var(--border-soft);
            border-radius: 14px;
            padding: 0.85rem 1.1rem;
            margin-bottom: 1.5rem;
            display: flex;
            flex-wrap: wrap;
            gap: 0.5rem 1rem;
            align-items: center;
        }
        .status-strip strong { font-size: 0.8rem; color: var(--muted); margin-right: 0.25rem; }
        .chip {
            display: inline-flex;
            align-items: center;
            padding: 0.25rem 0.65rem;
            border-radius: 999px;
            font-size: 0.78rem;
            font-weight: 600;
            background: var(--card);
            border: 1px solid var(--card-edge);
        }
        .chip-reading { color: var(--reading); border-color: rgba(74, 111, 165, 0.35); }
        .chip-done { color: var(--success); border-color: rgba(46, 107, 78, 0.35); }
        .chip-want { color: var(--want); border-color: rgba(139, 107, 156, 0.35); }
        .chip-default { color: var(--muted); }
        [data-theme="dark"] .status-strip { border-color: var(--border); }
        .user-card {
            background: var(--surface);
            border: 1px solid var(--border-soft);
            border-radius: 18px;
            margin-bottom: 1.25rem;
            overflow: hidden;
            box-shadow: 0 2px 16px rgba(45, 40, 30, 0.06);
            transition: opacity 0.2s, transform 0.2s;
        }
        [data-theme="dark"] .user-card { border-color: var(--border); box-shadow: 0 4px 24px rgba(0,0,0,0.4); }
        .user-card.hidden { display: none; }
        .user-head {
            padding: 1.1rem 1.25rem;
            background: var(--card);
            border-bottom: 1px solid var(--border);
            display: flex;
            flex-wrap: wrap;
            gap: 1rem;
            align-items: flex-start;
            justify-content: space-between;
        }
        .user-identity { display: flex; gap: 0.85rem; align-items: flex-start; }
        .avatar {
            width: 48px; height: 48px;
            border-radius: 14px;
            background: var(--primary);
            color: var(--on-primary);
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: 700;
            font-size: 1rem;
            flex-shrink: 0;
        }
        .user-head strong { font-size: 1.05rem; color: var(--primary); display: block; }
        .user-head .email { color: var(--muted); font-size: 0.88rem; margin-top: 0.15rem; }
        .badge {
            display: inline-block;
            font-size: 0.68rem;
            font-weight: 700;
            letter-spacing: 0.05em;
            text-transform: uppercase;
            padding: 0.28rem 0.55rem;
            border-radius: 8px;
            background: var(--tertiary);
            color: var(--on-tertiary);
        }
        .badge.off { background: var(--surface-variant); color: var(--muted); }
        [data-theme="dark"] .badge.off { color: var(--muted); }
        .stats { font-size: 0.85rem; color: var(--muted); margin-top: 0.35rem; }
        .meta-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
            gap: 0.75rem 1rem;
            font-size: 0.86rem;
            padding: 0.9rem 1.25rem 0.5rem;
        }
        .meta-grid div span { color: var(--muted); display: block; font-size: 0.68rem; text-transform: uppercase; letter-spacing: 0.05em; font-weight: 600; margin-bottom: 0.15rem; }
        details.book-block { border-top: 1px solid var(--border); }
        details.book-block > summary {
            list-style: none;
            cursor: pointer;
            padding: 0.75rem 1.25rem;
            font-weight: 600;
            font-size: 0.88rem;
            color: var(--primary);
            display: flex;
            align-items: center;
            justify-content: space-between;
            user-select: none;
        }
        details.book-block > summary::-webkit-details-marker { display: none; }
        details.book-block > summary::after { content: "▼"; font-size: 0.65rem; opacity: 0.6; transition: transform 0.2s; }
        details.book-block:not([open]) > summary::after { transform: rotate(-90deg); }
        details.book-block > summary:hover { background: rgba(47, 71, 54, 0.06); }
        [data-theme="dark"] details.book-block > summary:hover { background: rgba(139, 191, 164, 0.1); }
        .book-wrap { padding: 0 0.75rem 1rem 1rem; overflow-x: auto; }
        table { width: 100%; border-collapse: collapse; font-size: 0.8rem; }
        th, td { padding: 0.5rem 0.55rem; text-align: left; border-bottom: 1px solid var(--border); vertical-align: middle; }
        th {
            background: var(--table-head-bg);
            color: var(--primary);
            font-weight: 600;
            white-space: nowrap;
        }
        [data-theme="dark"] th { color: var(--table-head-fg); }
        tbody tr:hover { background: var(--table-row-hover); }
        .pill {
            display: inline-block;
            padding: 0.15rem 0.45rem;
            border-radius: 6px;
            font-size: 0.72rem;
            font-weight: 600;
        }
        .pill-completed { background: rgba(46, 107, 78, 0.15); color: var(--success); }
        .pill-reading { background: rgba(74, 111, 165, 0.15); color: var(--reading); }
        .pill-want { background: rgba(139, 107, 156, 0.15); color: var(--want); }
        .pill-other { background: rgba(92, 83, 72, 0.12); color: var(--muted); }
        .progress-wrap { min-width: 72px; }
        .progress-bar {
            height: 6px;
            border-radius: 99px;
            background: var(--border);
            overflow: hidden;
            margin-top: 0.2rem;
        }
        .progress-bar > i {
            display: block;
            height: 100%;
            border-radius: 99px;
            background: var(--primary);
        }
        .empty-books { padding: 0.75rem 1.25rem; color: var(--muted); font-size: 0.9rem; }
        form.inline { display: inline; margin: 0; }
        .site-footer {
            margin-top: auto;
            padding: 1rem 1.5rem;
            text-align: center;
            font-size: 0.78rem;
            color: var(--muted);
            border-top: 1px solid var(--border-soft);
            background: var(--surface);
        }
        [data-theme="dark"] .site-footer { border-color: var(--border); }
        @media print {
            .top-header .header-actions, .toolbar, .theme-toggle-login { display: none !important; }
            .user-card { break-inside: avoid; }
        }
    </style>
</head>
<body>
    <header class="top-header">
        <div class="top-header-inner">
            <div>
                <h1>Shelfie admin</h1>
                <div class="tagline">
                    Library overview · Signed in as <?= h($adminSelf['first_name'] !== '' ? $adminSelf['first_name'] : $adminSelf['email']) ?>
                </div>
            </div>
            <div class="header-actions">
                <button type="button" class="btn btn-ghost" id="btnTheme" title="Light / dark">◐ Theme</button>
                <button type="button" class="btn btn-ghost" id="btnExpand">Collapse all shelves</button>
                <a class="btn btn-primary-lite" href="admin_dashboard.php?export=json">⬇ Export JSON</a>
                <form class="inline" method="post" action="admin_dashboard.php">
                    <input type="hidden" name="logout" value="1">
                    <button class="btn btn-ghost" type="submit">Log out</button>
                </form>
            </div>
        </div>
    </header>

    <main class="wrap">
        <div class="stats-grid">
            <div class="stat-card">
                <div class="label">Readers</div>
                <div class="value"><?= (int)$nUsers ?></div>
                <div class="hint">Registered accounts</div>
            </div>
            <div class="stat-card">
                <div class="label">Books on shelves</div>
                <div class="value"><?= (int)$totalBookRows ?></div>
                <div class="hint">Total rows in library</div>
            </div>
            <?php if ($hasJournals): ?>
            <div class="stat-card">
                <div class="label">Journal entries</div>
                <div class="value"><?= (int)$sumJournals ?></div>
                <div class="hint">Across all users</div>
            </div>
            <?php endif; ?>
            <div class="stat-card">
                <div class="label">Admin accounts</div>
                <div class="value"><?= (int)$nAdmins ?></div>
                <div class="hint">Users with admin flag</div>
            </div>
        </div>

        <?php if (!empty($statusTotals)): ?>
        <div class="status-strip">
            <strong>All books by status</strong>
            <?php
            foreach ($statusTotals as $label => $cnt) {
                $l = strtolower($label);
                $cls = 'chip-default';
                if (strpos($l, 'complete') !== false) {
                    $cls = 'chip-done';
                } elseif (strpos($l, 'read') !== false && strpos($l, 'want') === false) {
                    $cls = 'chip-reading';
                } elseif (strpos($l, 'want') !== false) {
                    $cls = 'chip-want';
                }
                echo '<span class="chip ' . h($cls) . '">' . h($label) . ': ' . (int)$cnt . '</span>';
            }
            ?>
        </div>
        <?php endif; ?>

        <div class="toolbar">
            <div class="search-wrap">
                <input type="search" id="userFilter" placeholder="Filter by name, email, country, or user ID…" autocomplete="off">
            </div>
        </div>

        <?php if (empty($users)): ?>
            <p>No users found.</p>
        <?php else: ?>
            <?php foreach ($users as $uid => $u):
                $initials = '';
                $fn = trim((string)($u['first_name'] ?? ''));
                $ln = trim((string)($u['last_name'] ?? ''));
                if ($fn !== '') {
                    $initials .= strtoupper(substr($fn, 0, 1));
                }
                if ($ln !== '') {
                    $initials .= strtoupper(substr($ln, 0, 1));
                }
                if ($initials === '') {
                    $initials = substr((string)($u['email'] ?? '?'), 0, 1);
                }
                $displayName = trim($fn . ' ' . $ln) ?: '(no name)';
                $searchBlob = h(user_search_blob($u));
                ?>
                <article class="user-card" data-user-search="<?= $searchBlob ?>">
                    <div class="user-head">
                        <div class="user-identity">
                            <div class="avatar" aria-hidden="true"><?= h($initials) ?></div>
                            <div>
                                <strong>#<?= (int)$u['id'] ?> — <?= h($displayName) ?></strong>
                                <div class="email"><?= h((string)$u['email']) ?></div>
                            </div>
                        </div>
                        <div>
                            <?php if ((int)$u['is_admin'] === 1): ?>
                                <span class="badge">Admin</span>
                            <?php else: ?>
                                <span class="badge off">Reader</span>
                            <?php endif; ?>
                            <div class="stats">
                                <?= (int)$u['book_count'] ?> books
                                <?php if ($hasJournals): ?> · <?= (int)$u['journal_count'] ?> journal<?php endif; ?>
                            </div>
                        </div>
                    </div>
                    <div class="meta-grid">
                        <div><span>Birthday</span><?= h((string)($u['birthday'] ?? '—')) ?></div>
                        <div><span>Country</span><?= h((string)($u['country'] ?? '—')) ?></div>
                        <div><span>Reading goal</span><?php
                            $gt = (string)($u['reading_goal_type'] ?? '');
                            $gv = (int)($u['reading_goal_value'] ?? 0);
                            echo $gt !== '' && $gv > 0 ? h($gt . ': ' . $gv) : '—';
                        ?></div>
                    </div>
                    <?php
                    $books = $booksByUser[$uid] ?? [];
                    $nb = count($books);
                    if ($nb === 0): ?>
                        <p class="empty-books">No books on this shelf yet.</p>
                    <?php else: ?>
                        <details class="book-block" open>
                            <summary>
                                <span>Shelf · <?= $nb ?> book<?= $nb === 1 ? '' : 's' ?></span>
                                <span style="font-weight:500;color:var(--muted);">Show / hide table</span>
                            </summary>
                            <div class="book-wrap">
                                <table>
                                    <thead>
                                        <tr>
                                            <th>ID</th>
                                            <th>Title</th>
                                            <th>Author</th>
                                            <th>Status</th>
                                            <th>Progress</th>
                                            <th>★</th>
                                            <th>Fav</th>
                                            <th>Rec</th>
                                            <th>Category</th>
                                            <th>Genres</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <?php foreach ($books as $b):
                                            $tp = max(1, (int)$b['total_pages']);
                                            $cp = (int)$b['current_page'];
                                            $pct = min(100, (int)round(100 * $cp / $tp));
                                            $stRaw = trim((string)($b['status'] ?? ''));
                                            $sl = strtolower($stRaw);
                                            $pClass = 'pill-other';
                                            if (strpos($sl, 'complete') !== false) {
                                                $pClass = 'pill-completed';
                                            } elseif (strpos($sl, 'want') !== false) {
                                                $pClass = 'pill-want';
                                            } elseif (strpos($sl, 'read') !== false) {
                                                $pClass = 'pill-reading';
                                            }
                                            ?>
                                            <tr>
                                                <td><?= (int)$b['id'] ?></td>
                                                <td><?= h((string)$b['title']) ?></td>
                                                <td><?= h((string)$b['author']) ?></td>
                                                <td><span class="pill <?= h($pClass) ?>"><?= h($stRaw ?: '—') ?></span></td>
                                                <td class="progress-wrap">
                                                    <?= $cp ?> / <?= (int)$b['total_pages'] ?>
                                                    <div class="progress-bar" title="<?= (int)$pct ?>%"><i style="width:<?= (int)$pct ?>%;"></i></div>
                                                </td>
                                                <td><?= h((string)$b['rating']) ?></td>
                                                <td><?= (int)$b['is_favorite'] ? '★' : '—' ?></td>
                                                <td><?= (int)($b['is_recommended'] ?? 0) ? 'Yes' : '—' ?></td>
                                                <td><?= h((string)$b['category']) ?></td>
                                                <td><?= h((string)($b['genres'] ?? '')) ?></td>
                                            </tr>
                                        <?php endforeach; ?>
                                    </tbody>
                                </table>
                            </div>
                        </details>
                    <?php endif; ?>
                </article>
            <?php endforeach; ?>
        <?php endif; ?>
    </main>
    <footer class="site-footer">
        Shelfie admin · <?= h(date('Y-m-d H:i')) ?>
    </footer>
    <script>
    (function(){
        var k = 'shelfie_admin_theme';
        if (localStorage.getItem(k) === 'dark') document.documentElement.setAttribute('data-theme','dark');
        document.getElementById('btnTheme').onclick = function(){
            if (document.documentElement.getAttribute('data-theme') === 'dark') {
                document.documentElement.removeAttribute('data-theme');
                localStorage.setItem(k,'light');
            } else {
                document.documentElement.setAttribute('data-theme','dark');
                localStorage.setItem(k,'dark');
            }
        };
        var inp = document.getElementById('userFilter');
        if (inp) {
            inp.addEventListener('input', function(){
                var q = this.value.trim().toLowerCase();
                document.querySelectorAll('.user-card[data-user-search]').forEach(function(card){
                    var hay = card.getAttribute('data-user-search') || '';
                    card.classList.toggle('hidden', q !== '' && hay.indexOf(q) === -1);
                });
            });
        }
        document.getElementById('btnExpand').onclick = function(){
            var toCollapse = this.textContent.indexOf('Collapse') !== -1;
            document.querySelectorAll('details.book-block').forEach(function(d){ d.open = !toCollapse; });
            this.textContent = toCollapse ? 'Expand all shelves' : 'Collapse all shelves';
        };
    })();
    </script>
</body>
</html>
