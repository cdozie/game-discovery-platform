UPDATE game_stats gs
SET average_rating = aggregated.average_rating
FROM (
    SELECT g.id AS game_id,
           ROUND(AVG(w.rating)::numeric, 1)::double precision AS average_rating
    FROM games g
    LEFT JOIN wishlist w ON w.gameinfo = g.id
    GROUP BY g.id
) aggregated
WHERE gs.game_id = aggregated.game_id;
