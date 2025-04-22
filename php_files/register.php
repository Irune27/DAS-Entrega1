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

// conectar a la base de datos
$conn = new mysqli($host, $user, $pass, $db);

// verificar que no haya errores al conectar
if ($conn->connect_error) {
    echo json_encode(['success' => false, 'message' => 'Connection error']);
    exit;
}

// obtener datos del cuerpo (JSON)
$data = json_decode(file_get_contents("php://input"), true);

// validar campos requeridos
if (!isset($data['username']) || !isset($data['password'])) {
    echo json_encode(['success' => false, 'message' => 'Missing data']);
    exit;
}

$username = $data['username'];
// hacer el hash de la contraseña
$password = password_hash($data['password'], PASSWORD_DEFAULT);

// verificar si el usuario ya existe (consulta preparada)
$user_check = $conn->prepare("SELECT id FROM Usuarios WHERE username = ?");
$user_check->bind_param("s", $username);
$user_check->execute();
$user_check->store_result();

if ($user_check->num_rows > 0) {
    echo json_encode(['success' => false, 'message' => 'The user already exists']);
    $user_check->close();
    exit;
}
$user_check->close();

// definir la foto de perfil por defecto (está en el servidor)
$default_image = 'user_images/default_user.png';

// insertar el nuevo usuario (consulta preparada)
$insert = $conn->prepare("INSERT INTO Usuarios (username, password, image) VALUES (?, ?, ?)");
$insert->bind_param("sss", $username, $password, $default_image);

if ($insert->execute()) {
    // propiedad del objeto mysqli --> devuelve el último ID auto_increment generado en esa conexión
    $user_id = $conn->insert_id;
    echo json_encode(['success' => true, 'message' => 'User registered', 'user_id' => $user_id]);
} else {
    echo json_encode(['success' => false, 'message' => 'Error when registering user']);
}

$insert->close();
$conn->close();
?>
