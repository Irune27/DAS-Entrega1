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
$response = ['success' => false];

// el archivo solo se procesa si la petición viene como POST
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    // validar campo requerido
    if (isset($_FILES['image'])) {
        $image = $_FILES['image'];

        // comprobar si ha habido algún error al subir la imagen
        if ($image['error'] === UPLOAD_ERR_OK) {
            // nombrar el archivo de imagen utilizando la fecha y hora actual
            $image_name = "recipe_" . date("Y-m-d") . "-" . date("h:i:sa") . ".jpg";
            $upload_dir = "recipe_images/";
            // especificar la ruta a la que se va a mover la imagen (directorio de imágenes)
            $upload_path = $upload_dir . $image_name;
            $response['upload_path'] = $upload_path;

            // mover la imagen subida
            if (move_uploaded_file($image['tmp_name'], $upload_path)) {
                // devolver el resultado junto con la ruta de la imagen , si todo es correcto
                $response['success'] = true;
                $response['message'] = "Image uploaded";
                $response['image_path'] = $upload_path;
            } else {
                echo $image['tmp_name'];
                $response['message'] = "Error when moving the image to the directory";
            }
        } else {
            $response['message'] = "Error when uploading the image";
        }
    } else {
        $response['message'] = "Missing data";
    }
} else {
    $response['message'] = "Method not allowed";
}

$conn->close();
echo json_encode($response);
?>
