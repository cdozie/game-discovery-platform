UPDATE wishlist
SET rating = ROUND((rating / 10.0)::numeric, 1)
WHERE rating IS NOT NULL
  AND rating > 10;

UPDATE game_stats gs
SET average_rating = COALESCE((
    SELECT AVG(w.rating)
    FROM wishlist w
    WHERE w.gameinfo = gs.game_id
), 0);
