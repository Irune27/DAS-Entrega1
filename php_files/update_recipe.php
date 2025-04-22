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
if (!isset($data['name']) || !isset($data['image']) || !isset($data['ingredients']) || !isset($data['steps']) || !isset($data['code'])) {
    echo json_encode(['success' => false, 'message' => 'Missing data']);
    exit;
}

$name = $data['name'];
$image = $data['image'];
$ingredients = $data['ingredients'];
$steps = $data['steps'];
$code = $data['code'];

// obtener la imagen antigua de la receta
$selectStmt = $conn->prepare("SELECT image FROM Recetas WHERE code = ?");
$selectStmt->bind_param("i", $code);
$selectStmt->execute();
$selectStmt->bind_result($oldImage);
$selectStmt->fetch();
$selectStmt->close();

// buscar la receta a actualizar mediante su código
$stmt = $conn->prepare("UPDATE Recetas SET name = ?, image = ?, ingredients = ?, steps = ? WHERE code = ?");
$stmt->bind_param("ssssi", $name, $image, $ingredients, $steps, $code);

if ($stmt->execute()) {
    // comprobar si la imagen ha cambiado y si se puede eliminar la antigua
    if ($oldImage !== $image && $oldImage !== 'recipe_images/default_image.jpg') {
        $filePath = __DIR__ . '/' . $oldImage;
        if (file_exists($filePath)) {
            unlink($filePath); // elimina el archivo
        }
    }
    echo json_encode(["success" => true, "message" => "Recipe updated"]);
} else {
    echo json_encode(["success" => false, "message" => "Error when updating recipe"]);
}

$stmt->close();
$conn->close();
?>
