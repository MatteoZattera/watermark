package watermark
import java.awt.Color
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

private fun String.reply() = println(this).run { readln() }
private fun File.itIfExistsOrNull() = if (this.exists()) this else null

private fun createBufferedImage(name: String, type: String): BufferedImage {

    val imageFile = File(name).itIfExistsOrNull() ?: throw InExistentFileException(name)
    val image = ImageIO.read(imageFile)
    if (image.colorModel.numColorComponents != 3) throw InvalidImageColorComponentsException(type)
    if (image.colorModel.pixelSize !in listOf(24, 32)) throw InvalidImageBitsPerPixelException(type)
    return image
}

private fun BufferedImage.addWatermark(
    watermarkImage: BufferedImage,
    outputImageType: Int,
    watermarkImageTransparency: Int,
    watermarkTransparencyPercentage: Int,
    watermarkPosition: Pair<Int, Int>,
    transparencyColor: Color? = null
): BufferedImage {

    val outputImage = BufferedImage(this.width, this.height, outputImageType)
    for (x in 0 until this.width) {
        for (y in 0 until this.height) {

            val i = Color(this.getRGB(x, y))
            val w = if (
                x in watermarkPosition.first until watermarkPosition.first + watermarkImage.width &&
                y in watermarkPosition.second until watermarkPosition.second + watermarkImage.height
            ) Color(watermarkImage.getRGB(x - watermarkPosition.first, y - watermarkPosition.second), watermarkImageTransparency == Transparency.TRANSLUCENT) else null
            val color = when {
                w == null || w.alpha == 0 || w == transparencyColor -> i
                w.alpha == 255 -> Color(
                    (watermarkTransparencyPercentage * w.red + (100 - watermarkTransparencyPercentage) * i.red) / 100,
                    (watermarkTransparencyPercentage * w.green + (100 - watermarkTransparencyPercentage) * i.green) / 100,
                    (watermarkTransparencyPercentage * w.blue + (100 - watermarkTransparencyPercentage) * i.blue) / 100
                )
                else -> throw InvalidWatermarkImageException()
            }
            outputImage.setRGB(x, y, color.rgb)
        }
    }
    return outputImage
}

fun main() {

    try {
        // Create the buffered images
        val image = createBufferedImage("Input the image filename:".reply(), "image")
        val watermarkImage = createBufferedImage("Input the watermark image filename:".reply(), "watermark")

        // Compare the dimensions of the two images
        if (image.width < watermarkImage.width || image.height < watermarkImage.height) throw InvalidWatermarkImageDimensionsException()

        // Set the watermark's alpha channel and the transparency color
        var watermarkImageTransparency = watermarkImage.transparency
        var transparencyColor: Color? = null
        if (watermarkImageTransparency == Transparency.TRANSLUCENT)
            watermarkImageTransparency = if ("Do you want to use the watermark's Alpha channel?".reply().lowercase() == "yes") Transparency.TRANSLUCENT else Transparency.OPAQUE
        else if ("Do you want to set a transparency color?".reply() == "yes") {
            val transparencyColorInput = "Input a transparency color ([Red] [Green] [Blue]):".reply()
            if (!transparencyColorInput.matches(Regex("\\d+ \\d+ \\d+"))) throw InvalidTransparencyColorException()
            val colors = transparencyColorInput.split(" ").map { it.toInt() }
            if (colors.any { it !in 0..255 }) throw InvalidTransparencyColorException()
            transparencyColor = Color(colors[0], colors[1], colors[2])
        }

        // Set the watermark transparency percentage
        val watermarkTransparencyPercentage = "Input the watermark transparency percentage (Integer 0-100):".reply()
            .toIntOrNull() ?: throw NumberFormatException("The transparency percentage isn't an integer number.")
        if (watermarkTransparencyPercentage !in 0..100) throw InvalidTransparencyPercentageException()

        // Set the watermark position and blend the two images
        val watermarkImagePositionMethod = "Choose the position method (single, grid):".reply()
        if (watermarkImagePositionMethod !in listOf("single", "grid")) throw InvalidWatermarkPositionMethodException()
        var outputImage: BufferedImage
        if (watermarkImagePositionMethod == "single") {
            val maxX = image.width - watermarkImage.width
            val maxY = image.height - watermarkImage.height
            val watermarkPositionInput = "Input the watermark position ([x 0-$maxX] [y 0-$maxY]):".reply()
            if (!watermarkPositionInput.matches(Regex("-?\\d+ -?\\d+"))) throw InvalidWatermarkPositionInputException()
            val watermarkPosition = watermarkPositionInput.split(" ").map { it.toInt() }
            if (watermarkPosition[0] !in 0..maxX || watermarkPosition[1] !in 0..maxY) throw InvalidWatermarkPositionException()
            outputImage = image.addWatermark(
                watermarkImage,
                BufferedImage.TYPE_INT_RGB,
                watermarkImageTransparency,
                watermarkTransparencyPercentage,
                watermarkPosition[0] to watermarkPosition[1],
                transparencyColor)
        } else {
            outputImage = image // bad
            for (x in 0 until image.width step watermarkImage.width) {
                for (y in 0 until image.height  step watermarkImage.height) {
                    outputImage = if (x == 0 && y == 0)
                        image.addWatermark(watermarkImage, BufferedImage.TYPE_INT_RGB, watermarkImageTransparency, watermarkTransparencyPercentage, 0 to 0, transparencyColor)
                    else
                        outputImage.addWatermark(watermarkImage, BufferedImage.TYPE_INT_RGB, watermarkImageTransparency, watermarkTransparencyPercentage, x to y, transparencyColor)
                }
            }
        }

        // Set the output image name
        val outputImageFilename = "Input the output image filename (jpg or png extension):".reply()
        if (!outputImageFilename.matches(Regex(".+\\.(jpg|png)"))) throw InvalidFileNameException()
        val outputImageFile = File(outputImageFilename)

        // Save the result image
        ImageIO.write(outputImage, outputImageFilename.takeLast(3), outputImageFile)
        println("The watermarked image $outputImageFilename has been created.")

    } catch (e: Exception) {
        println(e.message)
    }
}
