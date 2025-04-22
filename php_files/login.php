<?php
// depurar
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

// configurar conexión con la base de datos
$host = 'localhost';
$db = 'Xipalacios017_RecetasDB';
$user = 'Xipalacios017';
$pass = '6etfKWNui';

// preparar respuesta
header('Content-Type: application/json');
$response = ['success' => false, 'message' => ''];

// conectar a la base de datos
$conn = new mysqli($host, $user, $pass, $db);

// verificar que no haya errores al conectar
if ($conn->connect_error) {
    $response['message'] = 'Connection error';
    echo json_encode($response);
    exit;
}

// obtener datos del cuerpo (JSON)
$data = json_decode(file_get_contents('php://input'), true);

// validar campos requeridos
if (!isset($data['username']) || !isset($data['password'])) {
    $response['message'] = 'Missing data';
    echo json_encode($response);
    exit;
}

$username = $data['username'];
$password = $data['password'];

// buscar el hash de la contraseña correspondiente al nombre de usuario
$get_password = $conn->prepare("SELECT id, password FROM Usuarios WHERE username = ?");
$get_password->bind_param("s", $username);
$get_password->execute();
$get_password->store_result();

// si el usuario existe y la contraseña es correcta, iniciar sesión
if ($get_password->num_rows === 1) {
    $get_password->bind_result($user_id, $hashed_password);
    $get_password->fetch();

    if (password_verify($password, $hashed_password)) {
        $response['success'] = true;
        $response['message'] = 'Login successful';
        $response['user_id'] = $user_id;
    } else {
        $response['message'] = 'Invalid credentials';
    }
} else {
    $response['message'] = 'Invalid credentials';
}

$get_password->close();
$conn->close();

echo json_encode($response);
?>
