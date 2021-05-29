# Pixel Sprite Maker

This app procedurally generates pseudo-random pixel art sprites. Inspired by [this online generator](http://img.uninhabitant.com/spritegen.html).  

## Parameters
Edge density (`min`): Probability of a pixel at the edge of the image being filled. 0-1. Default 0.  
Center density (`max`): Probability of a pixel at the center of the image being filled. 0-1. Default 1.  
Bias (`bias`): Adjusts the rate at which the probability of a pixel being filled drops off as distance from the center increases. 0-1. Default 0.5.  
Gain (`gain`): Adjusts the rate at which the probability of a pixel being filled drops off as pixels approach and depart from the quartiles. 0-1. Default 0.5.  
Mirror X, Y, Positive, Negative: Probability of an image being mirrored about the x-axis, y-axis, positive diagonal, and negative diagonal. 0-1.  
Despeckle: Probability of a pixel with 0 Moore neighbors being cleared. 0-1.  
Despur: Probability of a pixel with 1 Moore neighbors being cleared. 0-1.  
Relax: Probability of a pixel with 7 Moore neighbors being filled. 0-1.  
Devoid: Probability of a pixel with 8 Moore neighbors being filled. 0-1.  
Color speed (`speed`): The rate at which colors propagate. >0.  
Mutation (`mutation`): The odds of a pixel color being chosen separately from its neighbors. 0-1.  
Color seeds: The number of randomly selected points to assign a random color. >=1.  
Colors: The number of colors in the palette. >=1.  

## Formula
For each pixel in the image, at a point `(x, y)` out of `(width, height)`, the distance in both the X and Y components from the center of the image are calculated as `1 - |(width or height) - (x or y) * 2| / (width or height)`. This will be a value that is 1 at the edges and 0 at the center. The X and Y distances are multiplied to produce an overall pixel distance between 0 and 1. The bias and gain functions are applied using the provided parameters on the distance to get a threshold equal to bias(gain(*distance*, *gain*), *bias*). The bias and gain functions are as follows:  
`bias(t, bias) = t ^ (log(bias) / log(0.5))`  
`gain(t, gain) = bias(t * 2, 1 - gain) / 2 when t <= 0.5`  
`gain(t, gain) = 1 - bias(2 - 2 * t, 1 - gain) / 2 when t > 0.5`  
Then, a random float between 0 and 1 is generated. If it is less than or equal to the threshold, the pixel is filled. Otherwise, it stays empty.  
A cellular automaton is run on each pixel using the despeckle, despur, relax, and devoid parameters. The number of surrounding filled cells (including diagonals) are counted for each pixel, and if there are 0 or 1 filled neighbors, a random number is compared to despeckle or despur, and, if it is less, the pixel is set to empty. The inverse is done using relax and devoid if there are 7 or 8 filled neighbors.  
To color the image, a number of random pixel coordinates are chosen equal to the color seeds parameter. At each coordinate, the pixel's color is set to a random selection from the color palette, and the pixel coordinate is enqueued onto an initially empty queue. So long as this queue is not empty, a coordinate is dequeued, and, for each neighbor not yet colored, a random number is compared to `speed`. If it is less or equal, the dequeued cell's color is copied to the neighbor, and the neighbor is enqueued. If any uncolored neighbors are skipped due to this random number check, the dequeued coordinate is re-enqueued onto the queue. So long as `speed` >= 0, this will eventually fill the image. When copying colors from one cell to another, if a random number is less than `mutation`, a new randomly selected palette color is chosen instead of the source cell's color. After the image is fully colored, empty cells have their colors reset to transparency.  
