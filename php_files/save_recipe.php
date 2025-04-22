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
if (!isset($data['name']) || !isset($data['image']) || !isset($data['ingredients']) || !isset($data['steps']) || !isset($data['user_id'])) {
    echo json_encode(['success' => false, 'message' => 'Missing data']);
    exit;
}

$name = $data['name'];
$image = $data['image'];
$ingredients = $data['ingredients'];
$steps = $data['steps'];
$user_id = $data['user_id'];

// insertar la nueva receta (consulta preparada)
$insert = $conn->prepare("INSERT INTO Recetas (Name, Image, Ingredients, Steps, user_id) VALUES (?, ?, ?, ?, ?)");
$insert->bind_param("ssssi", $name, $image, $ingredients, $steps, $user_id);

if ($insert->execute()) {
    // propiedad del objeto mysqli --> devuelve el último ID auto_increment generado en esa conexión
    $recipe_code = $conn->insert_id;
    echo json_encode(['success' => true, 'message' => 'Recipe saved', 'recipe_code' => $recipe_code]);
} else {
    echo json_encode(['success' => false, 'message' => 'Error when saving recipe']);
}

$insert->close();
$conn->close();
?>