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

// validar campo requerido
if (!isset($data['code'])) {
    echo json_encode(['success' => false, 'message' => 'Missing data']);
    exit;
}

$code = $data['code'];

// obtener la imagen antes de eliminar la receta, para borrarla también
$getImage = $conn->prepare("SELECT image FROM Recetas WHERE code = ?");
$getImage->bind_param("i", $code);
$getImage->execute();
$getImage->bind_result($imagePath);
$getImage->fetch();
$getImage->close();

// eliminar imagen si no es la por defecto
if ($imagePath && $imagePath !== 'recipe_images/default_image.jpg') {
    $filePath = __DIR__ . '/' . $imagePath;
    if (file_exists($filePath)) {
        unlink($filePath);
    }
}

// borrar la receta (consulta preparada)
$delete = $conn->prepare("DELETE FROM Recetas WHERE code = ?");
$delete->bind_param("i", $code);

if ($delete->execute()) {
    echo json_encode(['success' => true, 'message' => 'Recipe deleted']);
} else {
    echo json_encode(['success' => false, 'message' => 'Error when deleting recipe']);
}

$delete->close();
$conn->close();
?>