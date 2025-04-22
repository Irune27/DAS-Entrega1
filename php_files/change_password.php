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

// conectar a la base de datos
$conn = new mysqli($host, $user, $pass, $db);

// verificar que no haya errores al conectar
if ($conn->connect_error) {
    echo json_encode(['success' => false, 'message' => 'Connection error']);
    exit;
}

// preparar respuesta
header('Content-Type: application/json');
$response = ['success' => false, 'message' => ''];

// el archivo solo se procesa si la petición viene como POST
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $input = json_decode(file_get_contents("php://input"), true);
    $action = $input['action'] ?? '';
    $user_id = $input['user_id'] ?? '';

    // validar el campo requerido user_id
    if (empty($user_id)) {
        $response['message'] = "Missing data";
        echo json_encode($response);
        exit;
    }

    // si la acción a realizar es comprobar la contraseña actual
    if ($action === 'verify_password') {
        $current_password = $input['current_password'] ?? '';

        // buscar al usuario por id
        $stmt = $conn->prepare("SELECT password FROM Usuarios WHERE id = ?");
        $stmt->bind_param("i", $user_id);
        $stmt->execute();
        $stmt->bind_result($stored_password);
        if ($stmt->fetch()) {
            // comprobar si el hash de la contraseña guardada y la introducida coinciden
            if (password_verify($current_password, $stored_password)) {
                $response['success'] = true;
                $response['message'] = "Password is correct";
            } else {
                $response['message'] = "Incorrect current password";
            }
        } else {
            $response['message'] = "User not found";
        }
        $stmt->close();
    }

    // si la acción a realizar es cambiar la contraseña
    elseif ($action === 'change_password') {
        $new_password = $input['new_password'] ?? '';
        // hacer el hash de la nueva contraseña para guardarlo
        $hashed_password = password_hash($new_password, PASSWORD_DEFAULT);
        $stmt = $conn->prepare("UPDATE Usuarios SET password = ? WHERE id = ?");
        $stmt->bind_param("si", $hashed_password, $user_id);
        if ($stmt->execute()) {
            $response['success'] = true;
            $response['message'] = "Password changed";
        } else {
            $response['message'] = "Error when updating password";
        }
        $stmt->close();

    } else {
        $response['message'] = "Invalid action";
    }

} else {
    $response['message'] = "Method not allowed";
}

echo json_encode($response);
$conn->close();
?>
