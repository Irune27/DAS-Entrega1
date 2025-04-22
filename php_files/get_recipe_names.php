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
$response = ['success' => false, 'message' => '', 'recipes' => []];

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
$user_id = $data['user_id'] ?? null;

// si no se ha mandado el id del usuario, mandar un error
if (!$user_id) {
    $response['message'] = 'Missing data';
    echo json_encode($response);
    exit;
}

// conseguir los códigos, nombres e imágenes (rutas de imágenes) de todas las recetas
$stmt = $conn->prepare("SELECT Code, Name, Image FROM Recetas WHERE user_id = ?");
$stmt->bind_param("i", $user_id);
$stmt->execute();
$result = $stmt->get_result();
$recipes = [];

// guardar los resultados en una lista
while ($row = $result->fetch_assoc()) {
    $recipes[] = $row;
}

// enviar la respuesta
$response['recipes'] = $recipes;
echo json_encode(['success' => true, 'recipes_json' => json_encode($recipes)]);

$conn->close();
?>
