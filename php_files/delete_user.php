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
if (!isset($data['user_id'])) {
    echo json_encode(['success' => false, 'message' => 'Missing data']);
    exit;
}

$user_id = $data['user_id'];

// obtener todas las recetas del usuario
$getRecipes = $conn->prepare("SELECT Image FROM Recetas WHERE user_id = ?");
$getRecipes->bind_param("i", $user_id);
$getRecipes->execute();
$getRecipes->bind_result($imagePath);

while ($getRecipes->fetch()) {
    if ($imagePath && $imagePath !== 'recipe_images/default_image.jpg') {
        // borrar las imágenes relacionadas con esas recetas del servidor SFTP
        $filePath = __DIR__ . '/' . $imagePath;
        if (file_exists($filePath)) {
            unlink($filePath);
        }
    }
}
$getRecipes->close();

// borrar las recetas del usuario
$deleteRecipes = $conn->prepare("DELETE FROM Recetas WHERE user_id = ?");
$deleteRecipes->bind_param("i", $user_id);
$deleteRecipes->execute();
$deleteRecipes->close();

// obtener la ruta a la foto de perfil del usuario
$getProfImage = $conn->prepare("SELECT image FROM Usuarios WHERE id = ?");
$getProfImage->bind_param("i", $user_id);
$getProfImage->execute();
$getProfImage->bind_result($profileImage);
$getProfImage->fetch();
$getProfImage->close();

// eliminar imagen si no es la por defecto
if ($profileImage && $profileImage !== 'user_images/default_user.png') {
    $profilePath = __DIR__ . '/' . $profileImage;
    if (file_exists($profilePath)) {
        unlink($profilePath);
    }
}


// borrar el usuario (consulta preparada)
$delete = $conn->prepare("DELETE FROM Usuarios WHERE id = ?");
$delete->bind_param("i", $user_id);

if ($delete->execute()) {
    echo json_encode(['success' => true, 'message' => 'User deleted']);
} else {
    echo json_encode(['success' => false, 'message' => 'Error when deleting user']);
}

$delete->close();
$conn->close();
?>