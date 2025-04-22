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
header("Content-Type: application/json");
$response = ['success' => false, 'message' => '', 'username' => '', 'profile_image' => ''];

// conectar a la base de datos
$conn = new mysqli($host, $user, $pass, $db);

// verificar que no haya errores al conectar
if ($conn->connect_error) {
    $response['message'] = 'Connection error';
    echo json_encode($response);
    exit;
}

// obtener datos del cuerpo (JSON)
$data = json_decode(file_get_contents("php://input"), true);

// validar campo requerido
if (!isset($data['user_id'])) {
    $response['message'] = 'Missing data';
    echo json_encode($response);
    exit;
}

$user_id = $data['user_id'];

// buscar el nombre de usuario y su foto de perfil según el id (consulta preparada)
$stmt = $conn->prepare("SELECT username, image FROM Usuarios WHERE id = ?");
$stmt->bind_param("i", $user_id);
$stmt->execute();
$result = $stmt->get_result();

// fetch_assoc --> obtener una fila de resultados como array asociativo
// se puede acceder a los datos por el nombre de la columna
if ($row = $result->fetch_assoc()) {
    $response['success'] = true;
    $response['username'] = $row['username'];
    $response['profile_image'] = $row['image'];
} else {
    $response['message'] = 'User not found';
}

echo json_encode($response);
?>
