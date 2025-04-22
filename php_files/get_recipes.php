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

// validar campo requerido
if (!isset($data['user_id'])) {
    echo json_encode(['success' => false, 'message' => 'Missing data']);
    exit;
}

$user_id = $data['user_id'];

// conseguir la información de todas las recetas del usuario
$stmt = $conn->prepare("SELECT Code, Name, Image, Ingredients, Steps FROM Recetas WHERE user_id = ?");
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
